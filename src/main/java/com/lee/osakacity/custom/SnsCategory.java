package com.lee.osakacity.custom;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SnsCategory {
    YOUTUBE("plane"),
    YOUTUBE_SHORT("short"),
    INSTAGRAM("instagram");

    private final String classValue;
}
