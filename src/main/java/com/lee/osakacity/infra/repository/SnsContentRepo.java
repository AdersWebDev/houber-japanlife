package com.lee.osakacity.infra.repository;

import com.lee.osakacity.infra.entity.SnsContent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SnsContentRepo extends JpaRepository<SnsContent , Long> {
    boolean existsByContent(String content);
}
