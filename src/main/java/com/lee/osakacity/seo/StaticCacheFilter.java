package com.lee.osakacity.seo;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.regex.Pattern;

/**
 * 정적 파일 캐시 헤더.
 * - 주소에 내용 해시가 붙은 파일(/css/main-0123...abcd.css): 내용이 바뀌면 주소가 바뀌므로 1년 캐시
 * - 해시 없는 주소(JS 안에 적힌 이미지 경로 등): 브라우저가 매번 변경 여부만 확인 (안 바뀌었으면 304, 내려받지 않음)
 */
@Component
public class StaticCacheFilter extends OncePerRequestFilter {
    private static final Pattern STATIC = Pattern.compile("^/(css|js|asset|admin-assets)/.+");
    private static final Pattern HASHED = Pattern.compile("-[0-9a-f]{32}\\.[A-Za-z0-9]+$");

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String method = request.getMethod();
        return !("GET".equals(method) || "HEAD".equals(method)) || !STATIC.matcher(request.getRequestURI()).matches();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        response.setHeader("Cache-Control", HASHED.matcher(request.getRequestURI()).find()
                ? "public, max-age=31536000, immutable"
                : "no-cache");
        chain.doFilter(request, response);
    }
}
