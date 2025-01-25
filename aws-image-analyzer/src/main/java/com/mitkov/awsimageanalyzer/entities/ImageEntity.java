package com.mitkov.awsimageanalyzer.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "image")
@Getter
@Setter
public class ImageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "url", nullable = false)
    private String url;

    @Column(name = "labels", nullable = false, columnDefinition = "TEXT")
    private String labels;

    @Column(name = "uploaded_at", nullable = false)
    private LocalDateTime uploadedAt;
}
