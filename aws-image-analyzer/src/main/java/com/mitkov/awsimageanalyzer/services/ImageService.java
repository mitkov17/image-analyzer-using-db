package com.mitkov.awsimageanalyzer.services;

import com.mitkov.awsimageanalyzer.entities.ImageEntity;
import com.mitkov.awsimageanalyzer.exceptions.FileUploadException;
import com.mitkov.awsimageanalyzer.repositories.ImageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.rekognition.RekognitionClient;
import software.amazon.awssdk.services.rekognition.model.*;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ImageService {

    @Value("${spring.aws.region}")
    private String region;

    @Value("${spring.aws.s3.bucket-name}")
    private String bucketName;

    private final S3Client s3Client;

    private final RekognitionClient rekognitionClient;

    private final ImageRepository imageRepository;

    public void uploadImage(MultipartFile file) {
        String fileName = file.getOriginalFilename();

        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileName)
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromBytes(file.getBytes()));

            DetectLabelsRequest detectLabelsRequest = DetectLabelsRequest.builder()
                    .image(Image.builder()
                            .s3Object(S3Object.builder()
                                    .bucket(bucketName)
                                    .name(fileName)
                                    .build())
                            .build())
                    .maxLabels(10)
                    .minConfidence(75F)
                    .build();

            DetectLabelsResponse detectLabelsResponse = rekognitionClient.detectLabels(detectLabelsRequest);

            String labels = detectLabelsResponse.labels().stream()
                    .map(Label::name)
                    .collect(Collectors.joining(","));

            ImageEntity imageEntity = new ImageEntity();
            imageEntity.setFileName(fileName);
            imageEntity.setUrl(String.format("https://%s.s3.%s.amazonaws.com/%s", bucketName, region, fileName));
            imageEntity.setLabels(labels);
            imageEntity.setUploadedAt(LocalDateTime.now());

            imageRepository.save(imageEntity);
        } catch (IOException e) {
            throw new FileUploadException("Failed to upload file: " + fileName, e);
        }
    }

    public List<String> searchImages(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return imageRepository.findAll().stream()
                    .map(ImageEntity::getUrl)
                    .collect(Collectors.toList());
        }

        return imageRepository.findByLabelsContainingIgnoreCase(keyword).stream()
                .map(ImageEntity::getUrl)
                .collect(Collectors.toList());
    }
}
