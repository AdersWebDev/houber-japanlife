package com.lee.osakacity.custom;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Category {
    hot_post("おすすめ", "지금 가장 인기 있는 콘텐츠를 확인해보세요!"),
    working_holiday("워킹 홀리데이", "일본 워킹 홀리데이를 위한 가이드를 모았습니다."),
    japan_study("유학 정보", "일본 유학을 준비하는 학생들을 위한 필수 정보를 모았습니다."),
    japan_life("생활 꿀팁", "일본에서의 일상생활과 유용한 정보를 한눈에 확인하세요."),
    houber_sns("영상 콘텐츠", "하우버의 새로운 소식을 확인해보세요."),
    all("전체 보기", "하우버의 모든 콘텐츠를 한곳에서 확인하세요.");

    private final String title;
    private final String description;

}
