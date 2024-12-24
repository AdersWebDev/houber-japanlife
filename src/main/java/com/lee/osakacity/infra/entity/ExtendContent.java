package com.lee.osakacity.infra.entity;

import com.lee.osakacity.dto.Category;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ExtendContent extends Item{
    @Column(nullable = false)
    private Category category;
    @Column(nullable = false)
    private String link;
    @OneToMany
    private List<File> fileList = new ArrayList<>();
}
