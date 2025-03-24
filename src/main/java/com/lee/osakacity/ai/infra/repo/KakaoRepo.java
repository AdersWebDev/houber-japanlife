package com.lee.osakacity.ai.infra.repo;

import com.lee.osakacity.ai.infra.KakaoLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface KakaoRepo extends JpaRepository<KakaoLog, Long> {
}
