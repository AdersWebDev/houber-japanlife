package com.lee.osakacity.admin;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Safelist;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 글 본문 HTML에서 안전한 태그·속성만 남긴다 (상세 페이지는 본문을 그대로 그리므로 꼭 필요)
 * 스크립트, 이벤트 속성(onclick 등), 위험한 링크는 지우고, 유튜브 외의 iframe도 지운다
 */
public final class HtmlSanitizer {
    private static final Pattern YOUTUBE = Pattern.compile("^https://www\\.youtube(-nocookie)?\\.com/embed/[A-Za-z0-9_-]{6,20}(\\?[A-Za-z0-9=&_-]*)?$");
    private static final Set<String> STYLE_PROPS = Set.of(
            "color", "background", "background-color", "text-align", "font-weight", "font-style",
            "text-decoration", "text-decoration-line", "aspect-ratio", "width", "max-width");

    private static final Safelist SAFELIST = new Safelist()
            .addTags("p", "br", "b", "strong", "i", "em", "u", "s", "span", "a", "h2", "h3", "ul", "ol", "li",
                    "table", "thead", "tbody", "tr", "th", "td", "blockquote", "hr", "div", "img", "iframe")
            .addAttributes(":all", "class", "style")
            .addAttributes("a", "href", "target", "rel")
            .addAttributes("img", "src", "alt")
            .addAttributes("td", "colspan", "rowspan")
            .addAttributes("th", "colspan", "rowspan")
            .addAttributes("iframe", "src", "title", "allowfullscreen", "frameborder", "allow")
            .addProtocols("a", "href", "http", "https", "mailto")
            .addProtocols("img", "src", "https", "http")
            .addProtocols("iframe", "src", "https");

    private HtmlSanitizer() {}

    public static String clean(String html) {
        if (html == null) return "";
        String cleaned = Jsoup.clean(html, "https://houber-japanlife.com", SAFELIST,
                new Document.OutputSettings().prettyPrint(false));
        Document doc = Jsoup.parseBodyFragment(cleaned);
        for (Element f : doc.select("iframe")) {
            if (!YOUTUBE.matcher(f.attr("src")).matches()) f.remove();
        }
        for (Element a : doc.select("a[href]")) {
            if (a.attr("href").startsWith("http")) {
                a.attr("target", "_blank");
                a.attr("rel", "noopener");
            }
        }
        for (Element e : doc.select("[style]")) {
            String style = filterStyle(e.attr("style"));
            if (style.isEmpty()) e.removeAttr("style");
            else e.attr("style", style);
        }
        doc.outputSettings().prettyPrint(false);
        return doc.body().html();
    }

    /** 허용한 CSS 속성만 남김 (url(), expression 등은 버림) */
    static String filterStyle(String style) {
        List<String> kept = new ArrayList<>();
        for (String decl : style.split(";")) {
            int i = decl.indexOf(':');
            if (i < 0) continue;
            String prop = decl.substring(0, i).trim().toLowerCase(Locale.ROOT);
            String value = decl.substring(i + 1).trim();
            String lower = value.toLowerCase(Locale.ROOT);
            if (!STYLE_PROPS.contains(prop) || value.isEmpty()
                    || lower.contains("url(") || lower.contains("expression") || lower.contains("javascript") || lower.contains("\\")) continue;
            kept.add(prop + ": " + value);
        }
        return String.join("; ", kept);
    }

    /** 본문 글자만 뽑아 요약에 씀 */
    public static String text(String html) {
        return Jsoup.parse(html == null ? "" : html).text().replaceAll("\\s+", " ").trim();
    }

    public static List<String> imageUrls(String html) {
        List<String> urls = new ArrayList<>();
        for (Element img : Jsoup.parseBodyFragment(html == null ? "" : html).select("img[src]")) urls.add(img.attr("src"));
        return urls;
    }
}
