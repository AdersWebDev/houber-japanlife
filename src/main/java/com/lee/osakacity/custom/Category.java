package com.lee.osakacity.custom;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Category {
    japan_review("하우버 리얼 입주후기 & 맨션정보", "하우버 이용자분들의 실 경험담과 하우버만의 다양한 이야기"),
    event("입주자면 무조건 받을 수 있는 혜택", "초기비용 줄이고, 혜택 늘리는 방법"),
    japan_property("한국과 다른 일본 부동산, 필수 체크!", "어려운 일본 부동산, 하우버가 쉽게 알려드려요"),
    working_holiday("워킹 홀리데이", "일본 워킹홀리데이를 위한 가이드를 모았습니다."),
    japan_life("일본에서 살아보니 알게 된 진짜 정보", "일본에서의 일상생활과 유용한 정보를 한눈에 확인하세요."),
    // DB에는 순서 번호(ordinal)로 저장되므로 새 카테고리는 항상 맨 뒤에 추가
    houber_news("하우버 새 소식", "하우버가 직접 전하는 입주 후기와 새 소식");

    private final String title;
    private final String description;

}
