package com.lee.osakacity.seo;

import java.util.Map;

/**
 * 같은 내용이 여러 번 올라간 글: 예전 주소 → 남길 글 주소 (301 영구 이동).
 * 검색엔진에 쌓인 평가를 남길 글 하나로 모으기 위함. 사이트맵·RSS에서도 뺀다.
 */
public final class DuplicatePosts {
    private DuplicatePosts() {}

    public static final Map<Long, Long> REDIRECTS = Map.of(
            101L, 98L,   // 워홀오기전 필수!! 체크리스트 A to Z
            114L, 120L,  // 일본 취업, 2주만에 신축맨션 계약 성공기!
            115L, 120L
    );
}
