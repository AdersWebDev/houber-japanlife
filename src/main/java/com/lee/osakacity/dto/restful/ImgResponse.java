package com.lee.osakacity.dto.restful;

import com.lee.osakacity.infra.entity.File;
import lombok.Getter;

@Getter
public class ImgResponse {
    private final Long id;
    private final String url;

    public ImgResponse (File file) {
        this.id = file.getId();
        this.url = file.getFileUrl();
    }
}
