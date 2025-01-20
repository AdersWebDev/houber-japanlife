package com.lee.osakacity.infra.repository;

import com.lee.osakacity.infra.entity.File;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;


public interface S3FileRepo extends JpaRepository<File, Long> {
    List<File> findAllByIsUsed(boolean used);
}
