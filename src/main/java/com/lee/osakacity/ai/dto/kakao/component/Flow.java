package com.lee.osakacity.ai.dto.kakao.component;

import lombok.Data;

@Data
public class Flow {
    private Block lastBlock;
    private Trigger trigger;

    @Data
    public static class Trigger {
        private String type;
        private Block referrerBlock;
    }
}
