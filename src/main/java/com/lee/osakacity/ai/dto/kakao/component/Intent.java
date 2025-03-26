package com.lee.osakacity.ai.dto.kakao.component;

import lombok.Data;

@Data
public class Intent {
    private String id;
    private String name;
    private Extra extra;

    @Data
    public static class Extra {
        private Reason reason;

        @Data
        public static class Reason {
            private int code;
            private String message;
        }
    }
}
