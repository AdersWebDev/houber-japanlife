package com.lee.osakacity.dto.mvc;

import com.lee.osakacity.custom.Category;
import com.lee.osakacity.infra.entity.Post;
import lombok.Getter;

import java.time.LocalDate;

@Getter
public class PostResponseDto {
    private final Category category;
    private final String title;
    private final String thumbnailUrl;
    private final String description;
    private final String keyword;
    private final LocalDate modifiedDate;
    private final int view;
    private final String content;

    public PostResponseDto(Post post) {
        this.category = post.getCategory();
        this.modifiedDate = post.getModifiedDate();
        this.title = post.getTitle();
        this.thumbnailUrl = post.getThumbnailUrl();
        this.description = post.getDescription();
        this.keyword = post.getKeyword();
        this.content = post.getContent();
        this.view = post.getView();
    }
}
