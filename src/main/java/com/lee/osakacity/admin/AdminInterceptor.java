package com.lee.osakacity.admin;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;

/**
 * 관리자만 쓸 수 있는 주소를 막는다
 * - /admin/** 화면과 API (로그인 화면 제외)
 * - 글·사진을 바꾸는 기존 API: /post(GET 제외), /file, /update, /scarp
 * 저장·삭제처럼 서버를 바꾸는 요청은 로그인 때 받은 토큰(X-Admin-Token 헤더 또는 _token 값)도 확인
 */
@Component
@RequiredArgsConstructor
public class AdminInterceptor implements HandlerInterceptor {
    private final AdminAuthService auth;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String path = request.getRequestURI();
        String method = request.getMethod();
        if (path.equals("/admin/login")) return true;
        if ((path.equals("/post") || path.startsWith("/post/")) && method.equals("GET")) return true; // 목록·검색은 공개

        HttpSession session = request.getSession(false);
        boolean page = path.startsWith("/admin") && !path.startsWith("/admin/api/") && method.equals("GET");
        if (!auth.isLoggedIn(session)) {
            if (page) {
                response.sendRedirect("/admin/login");
            } else {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            }
            return false;
        }
        if (!method.equals("GET") && !method.equals("HEAD")) {
            String sent = request.getHeader("X-Admin-Token");
            if (sent == null) sent = request.getParameter("_token");
            String expected = auth.token(session);
            if (sent == null || expected == null
                    || !MessageDigest.isEqual(sent.getBytes(StandardCharsets.UTF_8), expected.getBytes(StandardCharsets.UTF_8))) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN);
                return false;
            }
        }
        return true;
    }
}
