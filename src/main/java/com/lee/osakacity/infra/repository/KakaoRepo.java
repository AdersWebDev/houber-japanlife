package com.lee.osakacity.infra.repository;

import com.lee.osakacity.infra.entity.KakaoLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface KakaoRepo extends JpaRepository<KakaoLog, Long> {
}
