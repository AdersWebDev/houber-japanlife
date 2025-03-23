package com.lee.osakacity.ai.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Status {
    T1("空室","입주 가능"),
    T2("退去予定","퇴실예정 방"),//퇴거예정
    T3("清掃中","청소중 / 곧 입주 가능"),//청소중
    T4("内装中","타 고객 구경 중"),//내장중
    T5("審査中","다른 분이 신청 중"),//심사중
    T6("商談中","입주가 어려운 곳"),//상담중
    T7("新築","입주 가능한 신축 건물"),//신축
    T8("建築中","아래의 건축 완료일을 참고하세요."),//건축중
    T9("不可","입주 불가");//불가

    private final String title;
    private final String description;
}
