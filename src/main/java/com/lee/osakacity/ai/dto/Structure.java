package com.lee.osakacity.ai.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Structure {
    REBAR("철근 콘크리트"),
    IRON_FRAME("철골조"),
    WOOD_CARVING("목조"),
    OTHER("문의 요망");

    private final String title;
}
