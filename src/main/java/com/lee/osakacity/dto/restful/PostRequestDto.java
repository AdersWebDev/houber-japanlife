package com.lee.osakacity.dto.restful;

import com.lee.osakacity.custom.Category;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class PostRequestDto {
    private List<Long> imgList = new ArrayList<>();
    private Category category;
    private String thumbnailUrl;
    private String title;
    private String description;
    private String keyword;
    private String content;
}
