package com.lee.osakacity.custom;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Category {
    japan_review("하우버 입주자 분들의 리얼 후기", "하우버 이용자분들의 실제 경험담을 통해 다양한 이야기를 만나보세요."),
    event("입주자면 무조건 받을 수 있는 혜택", "초기비용 줄이고, 혜택 늘리는 방법"),
    japan_property("한국과 다른 일본 부동산, 필수 체크!", "어려운 일본 부동산, 하우버가 쉽게 알려드려요"),
    working_holiday("워킹 홀리데이", "일본 워킹홀리데이를 위한 가이드를 모았습니다."),
    japan_life("일본에서 살아보니 알게 된 진짜 정보", "일본에서의 일상생활과 유용한 정보를 한눈에 확인하세요.");

    private final String title;
    private final String description;

}
