package com.lee.osakacity.ai.dto.kakao.component;

import lombok.Data;

import java.util.Map;

@Data
public class User {
    private String id;
    private String type;
    private Map<String, Object> properties;
}

