package com.lee.osakacity.ai.infra;

import org.springframework.data.jpa.repository.JpaRepository;

public interface KakaoRepo extends JpaRepository<KakaoLog, Long> {
}
