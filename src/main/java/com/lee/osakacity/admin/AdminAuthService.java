package com.lee.osakacity.admin;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 관리자 로그인 (관리자 1명, 비밀번호 하나)
 * 비밀번호는 저장소가 아니라 서버의 ~/japanlife/admin.yml (admin.password)에 둔다
 */
@Service
public class AdminAuthService {
    static final String SESSION_ADMIN = "ADMIN";
    static final String SESSION_TOKEN = "ADMIN_TOKEN";
    private static final int MAX_FAILS = 5;
    private static final long LOCK_MILLIS = 10 * 60 * 1000L;

    private final byte[] passwordHash;
    private final SecureRandom random = new SecureRandom();
    private final Map<String, long[]> fails = new ConcurrentHashMap<>(); // 접속 주소 -> {실패 횟수, 잠금 해제 시각}

    public AdminAuthService(@Value("${admin.password:}") String password) {
        this.passwordHash = password.isBlank() ? null : sha256(password);
    }

    public boolean isConfigured() {
        return passwordHash != null;
    }

    public boolean isLoggedIn(HttpSession session) {
        return session != null && Boolean.TRUE.equals(session.getAttribute(SESSION_ADMIN));
    }

    public String token(HttpSession session) {
        return session == null ? null : (String) session.getAttribute(SESSION_TOKEN);
    }

    /** @return null이면 성공, 아니면 화면에 보여줄 오류 문구 */
    public String login(HttpServletRequest request, String password) {
        if (!isConfigured()) return "관리자 비밀번호가 서버에 설정되지 않았습니다.";
        String ip = clientIp(request);
        long now = System.currentTimeMillis();
        long[] f = fails.get(ip);
        if (f != null && f[1] > now) return "로그인을 여러 번 실패해 10분 동안 잠겼습니다.";

        if (password == null || !MessageDigest.isEqual(passwordHash, sha256(password))) {
            long[] next = (f == null || f[1] != 0 && f[1] <= now) ? new long[]{0, 0} : f;
            next[0]++;
            if (next[0] >= MAX_FAILS) { next[0] = 0; next[1] = now + LOCK_MILLIS; }
            fails.put(ip, next);
            return "비밀번호가 맞지 않습니다.";
        }
        fails.remove(ip);
        HttpSession old = request.getSession(false);
        if (old != null) old.invalidate();
        HttpSession session = request.getSession(true); // 로그인할 때마다 새 세션
        session.setMaxInactiveInterval(8 * 60 * 60);    // 글 쓰는 동안 끊기지 않게 8시간
        session.setAttribute(SESSION_ADMIN, true);
        byte[] t = new byte[24];
        random.nextBytes(t);
        session.setAttribute(SESSION_TOKEN, Base64.getUrlEncoder().withoutPadding().encodeToString(t));
        return null;
    }

    /**
     * 위조할 수 있는 X-Forwarded-For 헤더는 쓰지 않음. nginx 뒤에서는 모든 요청이 같은 주소라
     * 결과적으로 관리자 로그인 전체에 실패 제한이 걸림 (관리자 1명이라 괜찮음)
     */
    private static String clientIp(HttpServletRequest request) {
        return request.getRemoteAddr();
    }

    private static byte[] sha256(String s) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
