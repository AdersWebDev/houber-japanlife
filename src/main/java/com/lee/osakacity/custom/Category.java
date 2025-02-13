package com.lee.osakacity.custom;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Category {
    hot_post("おすすめ", "하우버에서 가장 인기 있는 콘텐츠를 확인해보세요!"),
    working_holiday("워킹 홀리데이", "일본 워킹홀리데이를 위한 가이드를 모았습니다."),
    japan_review("하우버 입주 후기!", "하우버 이용자분들의 실제 경험담을 통해 다양한 이야기를 만나보세요."),
    japan_life("일본 생활 정보", "일본에서의 일상생활과 유용한 정보를 한눈에 확인하세요."),
    houber_sns("영상 콘텐츠", "하우버의 새로운 소식을 확인해보세요."),
    all("전체 보기", "하우버의 모든 콘텐츠를 한곳에서 확인하세요.");

    private final String title;
    private final String description;

}
