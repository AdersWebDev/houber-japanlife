package com.lee.osakacity.dto;

import com.lee.osakacity.infra.entity.Guide;
import lombok.Getter;

@Getter
public class GuideResponseDto {
    private final String title;
    private final String description;
    private final String keyword;
    private final String content;

    public GuideResponseDto(Guide guide) {
        this.title = guide.getTitle();
        this.description = guide.getDescription();
        this.keyword = guide.getKeyword();
        this.content = guide.getContent();
    }
}
