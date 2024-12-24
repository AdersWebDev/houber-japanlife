package com.lee.osakacity.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class ContentHeading {
    private String id;
    private String text;
    private String level;
}
