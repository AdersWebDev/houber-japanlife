package com.lee.osakacity.admin;

import com.lee.osakacity.custom.Category;
import lombok.Getter;

import java.time.LocalDateTime;

/** 글 관리 목록 한 줄 (본문은 읽지 않음) */
@Getter
public class AdminPostRow {
    private final Long id;
    private final String title;
    private final Category category;
    private final boolean show;
    private final LocalDateTime modifiedDate;
    private final int view;
    private final String thumbnailUrl;

    public AdminPostRow(Long id, String title, Category category, boolean show, LocalDateTime modifiedDate, int view, String thumbnailUrl) {
        this.id = id;
        this.title = title;
        this.category = category;
        this.show = show;
        this.modifiedDate = modifiedDate;
        this.view = view;
        this.thumbnailUrl = thumbnailUrl;
    }
}
