package com.lee.osakacity.custom;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Category {
    HOT_POST("hot-post"),
    WORKING_HOLIDAY("working-holiday"),
    JAPAN_STUDY("japan-study"),
    JAPAN_LIFE("japan-life"),
    HOUBER_SNS_CONTENT("houber-sns-content"),
    VIEW_ALL("view-all");

    private final String value;
}
