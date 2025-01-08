package com.lee.osakacity.dto.restful;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
public class SiteMap {
    private final String link;
    private final LocalDate modifiedTime;

    public SiteMap (Long id, LocalDateTime time, String link) {
        this.link = link + id;
        this.modifiedTime = time.toLocalDate();
    }

}
