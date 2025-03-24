package com.lee.osakacity.ai.dto.custom;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

@RequiredArgsConstructor
@Getter
public enum RoomType {
    R1("1R"),
    K1("1K"),
    DK1("1DK"),
    LDK1("1LDK"),
    K2("2K"),
    DK2("2DK"),
    LDK2("2LDK"),
    K3("3K"),
    DK3("3DK"),
    LDK3("3LDK"),
    K4("4K"),
    DK4("4DK"),
    LDK4("4LDK"),
    K5("5K"),
    DK5("5DK"),
    LDK5("5LDK"),
    OTHER("문의 필요");

    private final String title;
    public static RoomType of(String title) {
        return Arrays.stream(RoomType.values())
                .filter(rt -> rt.getTitle().equalsIgnoreCase(title))
                .findFirst()
                .orElse(OTHER);
    }
}
