package com.lee.osakacity.infra.repository;

import com.lee.osakacity.dto.Category;
import com.lee.osakacity.infra.entity.Guide;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GuideRepo extends JpaRepository<Guide, Long> {
    List<Guide> findByCategoryOrderByIdAsc(Category category);

    List<Guide> findAllByOrderByIdAsc();
}
