package com.lee.osakacity.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class YouTubeSearchItem {
    private Id id;
    private Snippet snippet;

    @Data
    public static class Id {
        private String videoId;
    }
    @Data
    public static class Snippet {
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
        private LocalDateTime publishedAt;

        private String title;
        private String description;
        private Thumbnails thumbnails;

        @Data
        public static class Thumbnails {
            private ThumbnailDetail high;

            @Data
            public static class ThumbnailDetail {
                private String url;
                private int width;
                private int height;
            }
        }
    }
}
