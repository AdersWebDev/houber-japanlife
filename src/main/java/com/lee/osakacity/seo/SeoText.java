package com.lee.osakacity.seo;

import org.jsoup.Jsoup;

import java.util.regex.Pattern;

/**
 * 검색엔진에 보여줄 제목·설명 문구를 정리한다.
 * DB에 저장된 값은 바꾸지 않고, 화면(head)에 내보낼 때만 다듬는다.
 */
public final class SeoText {
    private SeoText() {}

    public static final String BRAND = "하우버";
    private static final int DESCRIPTION_MAX = 150;
    private static final int DESCRIPTION_MIN = 50;

    /** 인사말 사이사이의 문장부호·이모지·공백 */
    private static final String FILLER = "[\\s!~.,:;)(♡\\p{So}\\p{Sk}\\uFE0F\\u200D]*";
    /** "하우버입니다" / "하우버 부동산입니다" */
    private static final String BRAND_INTRO = "(저희는\\s*)?(하우버|houber)\\s*(부동산)?\\s*(입니다|이에요|예요)" + FILLER;
    /** 글 앞의 "안녕하세요, 하우버(부동산)입니다!~" 같은 인사말 (인사말만 빼고 본문은 남김) */
    private static final Pattern GREETING = Pattern.compile(
            "^\\s*(((안녕하세요|안녕하십니까)" + FILLER + "(" + BRAND_INTRO + ")?)|" + BRAND_INTRO + ")",
            Pattern.CASE_INSENSITIVE);
    /** 제목 앞뒤에 이미 붙어 있는 "하우버 |" / "| 하우버" */
    private static final Pattern TITLE_PREFIX = Pattern.compile("^\\s*(하우버|houber)\\s*[|｜:\\-]\\s*", Pattern.CASE_INSENSITIVE);
    private static final Pattern TITLE_SUFFIX = Pattern.compile("\\s*[|｜:\\-]\\s*(하우버|houber)\\s*$", Pattern.CASE_INSENSITIVE);
    /** 잘린 설명 끝의 "..." / "…" */
    private static final Pattern TRAILING_ELLIPSIS = Pattern.compile("\\s*(\\.{2,}|…)+\\s*$");

    /** 제목에서 "하우버 |" 같은 접두어·접미어를 뺀 순수 제목 */
    public static String cleanTitle(String title) {
        if (title == null) return "";
        String t = TITLE_PREFIX.matcher(title.trim()).replaceFirst("");
        return TITLE_SUFFIX.matcher(t).replaceFirst("").trim();
    }

    /** 브라우저 탭·검색 결과에 나오는 제목: "{제목} | 하우버" */
    public static String pageTitle(String title) {
        String clean = cleanTitle(title);
        return clean.isEmpty() ? BRAND : clean + " | " + BRAND;
    }

    /**
     * 메타 설명. 저장된 설명이 쓸 만하면 인사말만 빼서 쓰고,
     * 비어 있거나 "..."로 잘려 있거나 너무 짧으면 본문에서 새로 만든다.
     */
    public static String metaDescription(String savedDescription, String contentHtml) {
        String saved = normalize(savedDescription);
        boolean truncated = TRAILING_ELLIPSIS.matcher(saved).find();
        saved = stripGreeting(saved);
        if (!truncated && saved.length() >= DESCRIPTION_MIN) return cut(saved);

        String fromContent = contentHtml == null ? "" : stripGreeting(normalize(Jsoup.parse(contentHtml).text()));
        if (fromContent.length() >= DESCRIPTION_MIN) return cut(fromContent);

        // 둘 다 짧으면 더 긴 쪽
        String fallback = TRAILING_ELLIPSIS.matcher(saved).replaceFirst("");
        return cut(fallback.length() >= fromContent.length() ? fallback : fromContent);
    }

    private static String normalize(String s) {
        // 보이지 않는 공백(zero-width space 등)도 같이 정리
        return s == null ? "" : s.replaceAll("[\\u200B-\\u200D\\uFEFF]", "").replaceAll("\\s+", " ").trim();
    }

    private static String stripGreeting(String s) {
        return GREETING.matcher(s).replaceFirst("").trim();
    }

    /** 150자 안에서 문장이 끝나는 곳을 찾아 자르고, 없으면 단어 경계에서 자른 뒤 "…" */
    static String cut(String text) {
        if (text.length() <= DESCRIPTION_MAX) return text;
        String head = text.substring(0, DESCRIPTION_MAX);
        int end = -1;
        for (String mark : new String[]{". ", "! ", "? ", "다. ", "요. ", "~ "}) {
            int i = head.lastIndexOf(mark);
            if (i >= 0) end = Math.max(end, i + mark.length() - 1);
        }
        if (end > 60) return head.substring(0, end).trim();
        int space = head.lastIndexOf(' ');
        return (space > 60 ? head.substring(0, space) : head).trim() + "…";
    }
}
