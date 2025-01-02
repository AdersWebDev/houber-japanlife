package com.lee.osakacity.infra.entity;

import com.lee.osakacity.custom.Category;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;


@Entity
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class Post{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @CreatedDate
    @Column(updatable = false)
    private LocalDate createDate;
    @LastModifiedDate
    private LocalDate modifiedDate;

    @OneToMany
    private List<File> fileList = new ArrayList<>();

    @Column(nullable = false)
    private Category category;

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

    @Column(nullable = false)
    private String content;

    public void increaseView () {
        this.view++;
    }
}
