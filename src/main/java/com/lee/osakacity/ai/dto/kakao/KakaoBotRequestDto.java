package com.lee.osakacity.ai.dto.kakao;

import com.lee.osakacity.ai.dto.kakao.component.*;
import lombok.Data;

import java.util.List;

@Data
public class KakaoBotRequestDto {
    private Bot bot;
    private Intent intent;
    private Action action;
    private UserRequest userRequest;
    private List<Object> contexts;
    private Flow flow;
}
