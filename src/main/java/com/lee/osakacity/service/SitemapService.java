package com.lee.osakacity.service;

import com.lee.osakacity.dto.restful.RssMap;
import com.lee.osakacity.dto.restful.SiteMap;
import com.lee.osakacity.infra.entity.QPost;
import com.lee.osakacity.infra.entity.QSnsContent;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
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

        xml.append("""
            <url>
                <loc>https://houber-japanlife.com/</loc>
                <lastmod>2025-01-08</lastmod>
                <priority>1.0</priority>
            </url>
            <url>
                <loc>https://houber-japanlife.com/list?category=hot_post</loc>
                <lastmod>2025-01-08</lastmod>
                <priority>0.8</priority>
            </url>
            <url>
                <loc>https://houber-japanlife.com/list?category=working_holiday</loc>
                <lastmod>2025-01-08</lastmod>
                <priority>0.8</priority>
            </url>
            <url>
                <loc>https://houber-japanlife.com/list?category=japan_study</loc>
                <lastmod>2025-01-08</lastmod>
                <priority>0.8</priority>
            </url>
            <url>
                <loc>https://houber-japanlife.com/list?category=japan_life</loc>
                <lastmod>2025-01-08</lastmod>
                <priority>0.8</priority>
            </url>
            <url>
                <loc>https://houber-japanlife.com/list?category=houber_sns</loc>
                <lastmod>2025-01-08</lastmod>
                <priority>0.8</priority>
            </url>
            <url>
                <loc>https://houber-japanlife.com/list?category=all</loc>
                <lastmod>2025-01-08</lastmod>
                <priority>0.8</priority>
            </url>
            <url>
                <loc>https://houber-japanlife.com/search?keyword=houber</loc>
                <lastmod>2025-01-08</lastmod>
                <priority>0.8</priority>
            </url>
   """);
        list.stream()
                .map(s -> """
            <url>
                <loc>https://houber-japanlife.com/%s</loc>
                <lastmod>%s</lastmod>
                <priority>0.6</priority>
            </url>
        """.formatted(s.getLink(), s.getModifiedTime()))
                .forEach(xml::append);

        xml.append("</urlset>");
        return xml.toString().trim();

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
