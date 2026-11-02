package com.lee.osakacity.admin;

import com.lee.osakacity.custom.Category;

/** 글쓰기 화면에서 보내는 값 */
public record AdminPostRequest(Category category, String title, String content, String thumbnailUrl,
                               String description, boolean show) {}
