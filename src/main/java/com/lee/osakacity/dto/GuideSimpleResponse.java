package com.lee.osakacity.dto;

import com.lee.osakacity.infra.entity.Guide;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GuideSimpleResponse {
    private String title;
    private String thumbnailUrl;
    private String link;

    public GuideSimpleResponse(Guide guide) {
        this.title = guide.getTitle();
        this.thumbnailUrl = guide.getThumbnailUrl();
        this.link = "/guid/detail/"+guide.getId();
    }
    public GuideSimpleResponse(String a, String b, String c) {
        this.title = a;
        this.thumbnailUrl = b;
        this.link = c;
    }

}
