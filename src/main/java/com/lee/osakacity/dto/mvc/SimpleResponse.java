package com.lee.osakacity.dto.mvc;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class SimpleResponse {
    private final Long id;
    private final String title;
    private final String thumbnailUrl;
    private final int view;
    private final String link;
    private LocalDateTime cursorTime;

    public SimpleResponse(Long id, int view, String title, String thumbnailUrl, String link) {
        this.id = id;
        this.title = title;
        this.thumbnailUrl = thumbnailUrl;
        this.view = view;
        this.cursorTime = null;
        this.link = link +id;
    }

    public SimpleResponse(Long id, int view, String title, String thumbnailUrl, LocalDateTime cursorTime, String link) {
        this.id = id;
        this.title = title;
        this.thumbnailUrl = thumbnailUrl;
        this.view = view;
        this.cursorTime = cursorTime;
        this.link = link +id;
    }

}
