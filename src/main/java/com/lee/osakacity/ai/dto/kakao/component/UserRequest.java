package com.lee.osakacity.ai.dto.kakao.component;

import lombok.Data;

import java.util.Map;

@Data
public class UserRequest {
    private Block block;
    private User user;
    private String utterance;
    private Map<String, String> params;
    private String lang;
    private String timezone;
    private String callbackUrl;
}

