package com.lee.osakacity.seo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 검색엔진용 구조화 데이터(JSON-LD). head의 &lt;script type="application/ld+json"&gt;에 들어간다.
 * 전화번호·택건 면허 번호는 회사에서 받으면 ORGANIZATION에 추가.
 */
public final class StructuredData {
    private StructuredData() {}

    public static final String SITE = "https://houber-japanlife.com";
    private static final String LOGO = SITE + "/asset/logo.png";
    private static final ZoneId JST = ZoneId.of("Asia/Tokyo"); // +09:00
    private static final ObjectMapper JSON = new ObjectMapper();

    /** ISO 8601 + 시간대 (예: 2025-10-06T14:03:00+09:00). 서버 시간(일본)을 기준으로 저장된 값 */
    public static String isoDate(LocalDateTime time) {
        return time == null ? null : time.withNano(0).atZone(JST).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }

    private static Map<String, Object> organization() {
        Map<String, Object> o = new LinkedHashMap<>();
        o.put("@type", "RealEstateAgent");
        o.put("@id", SITE + "/#organization");
        o.put("name", "하우버");
        o.put("alternateName", "Houber");
        o.put("url", SITE + "/");
        o.put("logo", LOGO);
        o.put("image", LOGO);
        o.put("description", "오사카 한인 부동산 하우버. 일본 워킹홀리데이·유학·취업으로 오시는 분들의 집 구하기를 돕습니다.");
        Map<String, Object> address = new LinkedHashMap<>();
        address.put("@type", "PostalAddress");
        address.put("postalCode", "550-0004");
        address.put("addressRegion", "大阪府");
        address.put("addressLocality", "大阪市西区");
        address.put("streetAddress", "靱本町1-7-17 ラシーヌ四ツ橋7F");
        address.put("addressCountry", "JP");
        o.put("address", address);
        o.put("sameAs", List.of(
                "https://www.youtube.com/@Houber_Official",
                "https://www.instagram.com/osaka_houber/",
                "https://blog.naver.com/houber",
                "https://pf.kakao.com/_qTEPj"));
        return o;
    }

    /** 홈: 회사(부동산) + 웹사이트 */
    public static String home() {
        Map<String, Object> site = new LinkedHashMap<>();
        site.put("@type", "WebSite");
        site.put("@id", SITE + "/#website");
        site.put("name", "하우버");
        site.put("url", SITE + "/");
        site.put("inLanguage", "ko-KR");
        site.put("publisher", Map.of("@id", SITE + "/#organization"));
        return write(graph(List.of(organization(), site)));
    }

    /** 글 상세: 글(BlogPosting) + 이동 경로(홈 > 카테고리 > 글) */
    public static String article(String url, String headline, String description, String image,
                                 LocalDateTime published, LocalDateTime modified,
                                 String categoryName, String categoryUrl) {
        Map<String, Object> post = new LinkedHashMap<>();
        post.put("@type", "BlogPosting");
        post.put("@id", url + "#article");
        post.put("mainEntityOfPage", url);
        post.put("headline", headline.length() > 110 ? headline.substring(0, 110) : headline);
        if (description != null && !description.isBlank()) post.put("description", description);
        if (image != null && !image.isBlank()) post.put("image", List.of(image));
        post.put("datePublished", isoDate(published));
        post.put("dateModified", isoDate(modified != null ? modified : published));
        post.put("inLanguage", "ko-KR");
        if (categoryName != null) post.put("articleSection", categoryName);
        Map<String, Object> author = new LinkedHashMap<>();
        author.put("@type", "Organization");
        author.put("name", "하우버");
        author.put("url", SITE + "/");
        post.put("author", author);
        Map<String, Object> publisher = new LinkedHashMap<>();
        publisher.put("@type", "Organization");
        publisher.put("name", "하우버");
        publisher.put("logo", Map.of("@type", "ImageObject", "url", LOGO));
        post.put("publisher", publisher);

        Map<String, Object> crumbs = new LinkedHashMap<>();
        crumbs.put("@type", "BreadcrumbList");
        crumbs.put("itemListElement", categoryName == null
                ? List.of(crumb(1, "홈", SITE + "/"), crumb(2, headline, url))
                : List.of(crumb(1, "홈", SITE + "/"), crumb(2, categoryName, categoryUrl), crumb(3, headline, url)));
        return write(graph(List.of(post, crumbs)));
    }

    private static Map<String, Object> crumb(int position, String name, String item) {
        Map<String, Object> c = new LinkedHashMap<>();
        c.put("@type", "ListItem");
        c.put("position", position);
        c.put("name", name);
        c.put("item", item);
        return c;
    }

    private static Map<String, Object> graph(List<Map<String, Object>> nodes) {
        Map<String, Object> g = new LinkedHashMap<>();
        g.put("@context", "https://schema.org");
        g.put("@graph", nodes);
        return g;
    }

    private static String write(Object value) {
        try {
            // </script> 등으로 script 태그가 끊기지 않도록 '<'를 이스케이프
            return JSON.writeValueAsString(value).replace("<", "\\u003c");
        } catch (JsonProcessingException e) {
            return null;
        }
    }
}
