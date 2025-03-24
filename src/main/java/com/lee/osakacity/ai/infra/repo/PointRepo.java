package com.lee.osakacity.ai.infra.repo;

import com.lee.osakacity.ai.infra.PointLocation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PointRepo extends JpaRepository<PointLocation, Long> {
}
