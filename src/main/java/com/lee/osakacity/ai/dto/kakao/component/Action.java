package com.lee.osakacity.ai.dto.kakao.component;

import lombok.Data;

import java.util.Map;

@Data
public class Action {
    private String id;
    private String name;
    private Params params;
    private Map<String, DetailParam> detailParams;
    private Map<String, Object> clientExtra;
}
