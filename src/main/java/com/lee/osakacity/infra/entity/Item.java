package com.lee.osakacity.infra.entity;

import jakarta.persistence.*;
import lombok.Getter;

@MappedSuperclass
@Getter
public abstract class Item {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String thumbnailUrl;
    @Column(nullable = false)
    private String title;
}
