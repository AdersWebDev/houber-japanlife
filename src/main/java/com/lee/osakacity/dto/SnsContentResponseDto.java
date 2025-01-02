package com.lee.osakacity.dto;

import com.lee.osakacity.custom.SnsCategory;
import com.lee.osakacity.infra.entity.SnsContent;
import lombok.Getter;


@Getter
public class SnsContentResponseDto {
    private final Long id;

    private final SnsCategory snsCategory;

    private final String thumbnailUrl;

    private final String title;

    private final int view;

    private final String description;

    private final String keyword;

    private final String content;

    public SnsContentResponseDto(SnsContent snsContent) {
        this.id = snsContent.getId();
        this.snsCategory = snsContent.getSnsCategory();
        this.thumbnailUrl = snsContent.getThumbnailUrl();
        this.title = snsContent.getTitle();
        this.description = snsContent.getDescription();
        this.keyword = snsContent.getKeyword();
        this.content = snsContent.getContent();
        this.view = snsContent.getView();
    }
}
