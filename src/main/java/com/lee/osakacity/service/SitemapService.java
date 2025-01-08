package com.lee.osakacity.service;

import com.lee.osakacity.dto.restful.SiteMap;
import com.lee.osakacity.infra.entity.QPost;
import com.lee.osakacity.infra.entity.QSnsContent;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

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
    public String makeSiteMap () {
        List<SiteMap> list = siteMapQueryFactory();
        StringBuilder xml = new StringBuilder("""
        <?xml version="1.0" encoding="UTF-8"?>
        <urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">
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
        return xml.toString();

    }
}
