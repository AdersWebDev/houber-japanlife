package com.lee.osakacity.infra.entity;

import com.lee.osakacity.custom.SnsCategory;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;


@Entity
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SnsContent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime publishTime;

    @Column()
    private SnsCategory snsCategory;

    @Column(nullable = false)
    private String thumbnailUrl;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private int view;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false)
    private String keyword;

    @Column(nullable = false, unique = true)
    private String content;

    public void increaseView () {
        this.view++;
    }
    public void updateThumbnail(String url) {
        this.thumbnailUrl = url;
    }
}
