package com.lee.osakacity.dto.mvc;

import com.lee.osakacity.custom.Category;
import com.lee.osakacity.infra.entity.Post;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
public class PostResponseDto {
    private final Long id;
    private final Category category;
    private final String title;
    private final String thumbnailUrl;
    private final String description;
    private final String keyword;
    private final LocalDate modifiedDate;
    /** 게시 시각 (예전 글처럼 값이 없으면 수정 시각) */
    private final LocalDateTime publishedAt;
    private final LocalDateTime modifiedAt;
    private final int view;
    private final String content;

    public PostResponseDto(Post post) {
        this.id = post.getId();
        this.category = post.getCategory();
        this.modifiedDate = post.getModifiedDate().toLocalDate();
        this.modifiedAt = post.getModifiedDate();
        this.publishedAt = post.getCreateDate() != null ? post.getCreateDate() : post.getModifiedDate();
        this.title = post.getTitle();
        this.thumbnailUrl = post.getThumbnailUrl();
        this.description = post.getDescription();
        this.keyword = post.getKeyword();
        this.content = post.getContent();
        this.view = post.getView();
    }
}
