package com.lee.osakacity.dto.restful;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
@Getter
@Setter
public class RssMap {
    private final String title;
    private final String description;
    private final String link;
    private final LocalDateTime modifiedTime;

    public RssMap (String title, String description, Long id, LocalDateTime time, String link) {
        this.title = title;
        this.description = description;
        this.link = link + id;
        this.modifiedTime = time;
    }
}
