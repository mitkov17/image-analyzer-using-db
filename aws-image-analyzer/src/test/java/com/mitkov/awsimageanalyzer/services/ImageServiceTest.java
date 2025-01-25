package com.mitkov.awsimageanalyzer.services;

import com.mitkov.awsimageanalyzer.entities.ImageEntity;
import com.mitkov.awsimageanalyzer.exceptions.FileUploadException;
import com.mitkov.awsimageanalyzer.repositories.ImageRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.rekognition.RekognitionClient;
import software.amazon.awssdk.services.rekognition.model.DetectLabelsRequest;
import software.amazon.awssdk.services.rekognition.model.DetectLabelsResponse;
import software.amazon.awssdk.services.rekognition.model.Label;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ImageServiceTest {

    @Mock
    private S3Client s3Client;

    @Mock
    private RekognitionClient rekognitionClient;

    @Mock
    private ImageRepository imageRepository;

    @InjectMocks
    private ImageService imageService;

    @Test
    public void testUploadImageSuccess() throws IOException {
        MultipartFile mockFile = mock(MultipartFile.class);
        when(mockFile.getOriginalFilename()).thenReturn("test-image.jpg");
        when(mockFile.getBytes()).thenReturn("test content".getBytes());

        DetectLabelsResponse mockRekognitionResponse = DetectLabelsResponse.builder()
                .labels(Label.builder().name("cat").confidence(99.0f).build())
                .build();
        when(rekognitionClient.detectLabels((DetectLabelsRequest) any())).thenReturn(mockRekognitionResponse);

        assertDoesNotThrow(() -> imageService.uploadImage(mockFile));

        verify(s3Client, times(1)).putObject(any(PutObjectRequest.class), any(RequestBody.class));
        verify(imageRepository, times(1)).save(any(ImageEntity.class));
    }

    @Test
    public void testUploadImageFailure() throws IOException {
        MultipartFile mockFile = mock(MultipartFile.class);
        when(mockFile.getOriginalFilename()).thenReturn("test-image.jpg");
        when(mockFile.getBytes()).thenThrow(new IOException("Test exception"));

        FileUploadException exception = assertThrows(FileUploadException.class, () -> imageService.uploadImage(mockFile));
        assertTrue(exception.getMessage().contains("Failed to upload file"));

        verify(s3Client, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
        verify(imageRepository, never()).save(any(ImageEntity.class));
    }

    @Test
    public void testSearchImagesWithoutKeyword() {
        ImageEntity mockImage1 = new ImageEntity();
        mockImage1.setId(1L);
        mockImage1.setFileName("image1.jpg");
        mockImage1.setUrl("https://bucket-name.s3.region.amazonaws.com/image1.jpg");
        mockImage1.setLabels("cat,dog");
        mockImage1.setUploadedAt(LocalDateTime.now());

        ImageEntity mockImage2 = new ImageEntity();
        mockImage2.setId(2L);
        mockImage2.setFileName("image2.jpg");
        mockImage2.setUrl("https://bucket-name.s3.region.amazonaws.com/image2.jpg");
        mockImage2.setLabels("bird,tree");
        mockImage2.setUploadedAt(LocalDateTime.now());

        when(imageRepository.findAll()).thenReturn(List.of(mockImage1, mockImage2));

        List<String> result = imageService.searchImages(null);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.contains(mockImage1.getUrl()));
        assertTrue(result.contains(mockImage2.getUrl()));

        verify(imageRepository, times(1)).findAll();
    }

    @Test
    public void testSearchImagesWithKeyword() {
        ImageEntity mockImage = new ImageEntity();
        mockImage.setId(1L);
        mockImage.setFileName("image1.jpg");
        mockImage.setUrl("https://bucket-name.s3.region.amazonaws.com/image1.jpg");
        mockImage.setLabels("cat,dog");
        mockImage.setUploadedAt(LocalDateTime.now());

        when(imageRepository.findByLabelsContainingIgnoreCase("cat")).thenReturn(List.of(mockImage));

        List<String> result = imageService.searchImages("cat");

        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.contains(mockImage.getUrl()));

        verify(imageRepository, times(1)).findByLabelsContainingIgnoreCase("cat");
    }

    @Test
    public void testSearchImagesWithNonExistentKeyword() {
        when(imageRepository.findByLabelsContainingIgnoreCase("unicorn")).thenReturn(List.of());

        List<String> result = imageService.searchImages("unicorn");

        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(imageRepository, times(1)).findByLabelsContainingIgnoreCase("unicorn");
    }
}
