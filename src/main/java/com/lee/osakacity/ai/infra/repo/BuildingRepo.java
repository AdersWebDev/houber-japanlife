package com.lee.osakacity.ai.infra.repo;

import com.lee.osakacity.ai.infra.Building;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BuildingRepo extends JpaRepository<Building, Long> {
}
