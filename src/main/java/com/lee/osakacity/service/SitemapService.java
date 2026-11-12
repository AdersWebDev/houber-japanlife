package com.lee.osakacity.service;

import com.lee.osakacity.custom.Category;
import com.lee.osakacity.dto.restful.RssMap;
import com.lee.osakacity.dto.restful.SiteMap;
import com.lee.osakacity.infra.entity.QPost;
import com.lee.osakacity.infra.entity.QSnsContent;
import com.lee.osakacity.seo.DuplicatePosts;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SitemapService {
    private final JPAQueryFactory jpaQueryFactory;
    QPost qPost = QPost.post;
    QSnsContent qSnsContent = QSnsContent.snsContent;
    private List<SiteMap> siteMapQueryFactory() {
        List<SiteMap> data = new ArrayList<>();
        data.addAll(
                jpaQueryFactory
                        .select(Projections.constructor(SiteMap.class,
                        qPost.id,
                        qPost.modifiedDate,
                        Expressions.constant("detail/")
                ))
                .from(qPost)
                // 숨김 글(작성 중인 글 포함)과 다른 글로 옮긴 중복 글은 넣지 않음
                .where(qPost.isShow.isTrue().and(qPost.id.notIn(DuplicatePosts.REDIRECTS.keySet())))
                .fetch()
        );
        data.addAll(
                jpaQueryFactory
                        .select(Projections.constructor(SiteMap.class,
                                qSnsContent.id,
                                qSnsContent.publishTime,
                                Expressions.constant("detail/sns-content/")
                        ))
                        .from(qSnsContent)
                        .fetch()
        );
        return data;
    }
    private List<RssMap> RssQueryFactory() {
        List<RssMap> data = new ArrayList<>();
        data.addAll(
                jpaQueryFactory
                        .select(Projections.constructor(RssMap.class,
                                qPost.title,
                                qPost.description,
                                qPost.id,
                                qPost.modifiedDate,
                                Expressions.constant("detail/")
                        ))
                        .from(qPost)
                        .where(qPost.isShow.isTrue().and(qPost.id.notIn(DuplicatePosts.REDIRECTS.keySet())))
                        .limit(20)
                        .orderBy(qPost.modifiedDate.desc())
                        .fetch()
        );
        data.addAll(
                jpaQueryFactory
                        .select(Projections.constructor(RssMap.class,
                                qSnsContent.title,
                                qSnsContent.description,
                                qSnsContent.id,
                                qSnsContent.publishTime,
                                Expressions.constant("detail/sns-content/")
                        ))
                        .from(qSnsContent)
                        .limit(20)
                        .orderBy(qSnsContent.publishTime.desc())
                        .fetch()
        );
        // 날짜 기준 내림차순 정렬
        // 상위 20개만 선택
        return data.stream()
                .sorted(Comparator.comparing(RssMap::getModifiedTime).reversed()) // 날짜 기준 내림차순 정렬
                .limit(20) // 상위 20개만 선택
                .toList();
    }
    public String makeSiteMap () {
        List<SiteMap> list = siteMapQueryFactory();
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");

        // 📌 고정 페이지 (홈, 전체 글, 카테고리 목록). lastmod는 그 목록에 들어가는 가장 최근 글의 수정일
        //    priority는 Google이 쓰지 않으므로 넣지 않음. 검색 결과 페이지는 noindex라 넣지 않음
        Map<Category, LocalDate> latest = latestByCategory();
        LocalDateTime snsTime = jpaQueryFactory.select(qSnsContent.publishTime.max()).from(qSnsContent).fetchOne();
        LocalDate latestSns = snsTime == null ? null : snsTime.toLocalDate();
        LocalDate latestPost = latest.values().stream().max(Comparator.naturalOrder()).orElse(null);

        appendUrl(xml, "", max(latestPost, latestSns));
        appendUrl(xml, "list?category=all", latestPost);
        for (Category c : new Category[]{Category.houber_news, Category.japan_review, Category.working_holiday,
                Category.japan_property, Category.japan_life}) {
            // 입주후기 & 맨션정보 목록에는 맨션 영상도 나옴
            LocalDate d = c == Category.japan_review ? max(latest.get(c), latestSns) : latest.get(c);
            appendUrl(xml, "list?category=" + c.name(), d);
        }
        list.forEach(s -> appendUrl(xml, s.getLink(), s.getModifiedTime()));

        xml.append("</urlset>");
        return xml.toString().trim();

    }

    private void appendUrl(StringBuilder xml, String path, LocalDate lastmod) {
        xml.append("  <url>\n    <loc>https://houber-japanlife.com/").append(path).append("</loc>\n");
        if (lastmod != null) xml.append("    <lastmod>").append(lastmod).append("</lastmod>\n");
        xml.append("  </url>\n");
    }

    private static LocalDate max(LocalDate a, LocalDate b) {
        if (a == null) return b;
        if (b == null) return a;
        return a.isAfter(b) ? a : b;
    }

    /** 카테고리별 공개 글 중 가장 최근 수정일 */
    private Map<Category, LocalDate> latestByCategory() {
        Map<Category, LocalDate> result = new EnumMap<>(Category.class);
        for (Tuple t : jpaQueryFactory
                .select(qPost.category, qPost.modifiedDate.max())
                .from(qPost)
                .where(qPost.isShow.isTrue())
                .groupBy(qPost.category)
                .fetch()) {
            LocalDateTime time = t.get(qPost.modifiedDate.max());
            if (t.get(qPost.category) != null && time != null) result.put(t.get(qPost.category), time.toLocalDate());
        }
        return result;
    }

    public String makeRss() {
        List<RssMap> latestPosts = this.RssQueryFactory();
        StringBuilder rss = new StringBuilder();
        rss.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        rss.append("<rss version=\"2.0\">\n");
        rss.append("<channel>\n");
        rss.append("""

                <title>하우버 - 일본 생활의 모든 정보를 한눈에</title>
                <link>https://houber-japanlife.com</link>
                <description>일본 생활 정보, 워킹홀리데이, 유학등 모든 콘텐츠를 알려드려요!</description>
                <lastBuildDate>%s</lastBuildDate>
                <language>ko</language>
        """.formatted(ZonedDateTime.now().format(DateTimeFormatter.RFC_1123_DATE_TIME)));

        for (RssMap r : latestPosts) {
            rss.append("""
                <item>
                    <title>%s</title>
                    <link>https://houber-japanlife.com/%s</link>
                    <description>%s</description>
                    <pubDate>%s</pubDate>
                </item>
            """.formatted(
                    r.getTitle(),
                    r.getLink(),
                    r.getDescription(),
                    formatToRFC1123(r.getModifiedTime()) // RFC-1123 포맷 적용
            ));
        }

        rss.append("</channel>\n");
        rss.append("</rss>");

        return rss.toString().trim();
    }
    private String formatToRFC1123(LocalDateTime localDateTime) {
        return ZonedDateTime.of(localDateTime, ZoneId.systemDefault()) // 시스템 타임존 적용
                .format(DateTimeFormatter.RFC_1123_DATE_TIME);
    }
}
