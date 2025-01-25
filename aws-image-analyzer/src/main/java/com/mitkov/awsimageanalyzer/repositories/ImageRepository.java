package com.mitkov.awsimageanalyzer.repositories;

import com.mitkov.awsimageanalyzer.entities.ImageEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ImageRepository extends JpaRepository<ImageEntity, Long> {
    List<ImageEntity> findByLabelsContainingIgnoreCase(String keyword);
}
