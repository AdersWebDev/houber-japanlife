package com.lee.osakacity.infra.repository;

import com.lee.osakacity.custom.Category;
import com.lee.osakacity.infra.entity.Post;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PostRepo extends JpaRepository<Post, Long> {
    List<Post> findAllByCategoryOrderByIdAsc(Category category);

    List<Post> findAllByOrderByIdAsc();
}
