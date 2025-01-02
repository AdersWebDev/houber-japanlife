package com.lee.osakacity.dto.mvc;

import com.lee.osakacity.infra.entity.Post;
import com.lee.osakacity.infra.entity.SnsContent;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SimpleResponse {
    private String title;
    private String thumbnailUrl;
    private String link;

    public SimpleResponse(Post post) {
        this.title = post.getTitle();
        this.thumbnailUrl = post.getThumbnailUrl();
        this.link = "/detail/"+ post.getId();
    }
    public SimpleResponse(SnsContent snsContent) {
        this.title = snsContent.getTitle();
        this.thumbnailUrl = snsContent.getThumbnailUrl();
        this.link = "/detail/sns-content/"+snsContent.getId();
    }

}
