package com.lee.osakacity.dto.mvc;

import lombok.Getter;
import lombok.Setter;
import org.jsoup.Jsoup;

import java.time.LocalDateTime;

@Getter
@Setter
public class SearchResponseDto {
    private Long id;
    private String title;
    private String description;
    private int view;
    private String thumbnailUrl;
    private String link;
    private LocalDateTime dateTime;

    public SearchResponseDto(Long id, String title, int view, String content, String thumbnailUrl, LocalDateTime dateTime, String link) {
        this.id = id;
        this.title = title;
        this.view = view;
        this.description = this.setDescription(content);
        this.thumbnailUrl = thumbnailUrl;
        this.link = link + id;
        this.dateTime = dateTime;
    }
    private String setDescription(String content) {
        if (content != null && !content.isEmpty()) {
            // HTML 태그 제거
            String plainText = Jsoup.parse(content).text();

            // 50자 제한
            return plainText.length() > 90
                    ? plainText.substring(0, 80)
                    : plainText;
        } else {
            return "";
        }
    }
}
