package com.lee.osakacity.ai.infra;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RoomRepo extends JpaRepository<Room, Long> {
    List<Room> findByIdBetween(Long start, Long end);
}
