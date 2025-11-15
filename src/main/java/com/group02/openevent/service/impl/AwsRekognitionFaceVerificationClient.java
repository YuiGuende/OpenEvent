package com.group02.openevent.service.impl;

import com.group02.openevent.dto.face.FaceVerificationResult;
import com.group02.openevent.model.user.Customer;
import com.group02.openevent.service.FaceVerificationClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.rekognition.RekognitionClient;
import software.amazon.awssdk.services.rekognition.model.*;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.List;

/**
 * AWS Rekognition implementation of FaceVerificationClient for production
 * 
 * Required configuration:
 * - aws.rekognition.access-key
 * - aws.rekognition.secret-key
 * - aws.rekognition.region (default: ap-southeast-1)
 * - aws.rekognition.confidence-threshold (default: 80.0)
 */
@Service
@Slf4j
@ConditionalOnProperty(name = "face.recognition.provider", havingValue = "aws", matchIfMissing = false)
public class AwsRekognitionFaceVerificationClient implements FaceVerificationClient {
    
    private final RekognitionClient rekognitionClient;
    private final float confidenceThreshold;
    private final RestTemplate restTemplate;
    
    @Autowired
    public AwsRekognitionFaceVerificationClient(
            @Value("${aws.rekognition.access-key}") String accessKey,
            @Value("${aws.rekognition.secret-key}") String secretKey,
            @Value("${aws.rekognition.region:ap-southeast-1}") String region,
            @Value("${aws.rekognition.confidence-threshold:80.0}") float confidenceThreshold,
            RestTemplate restTemplate) {
        
        this.confidenceThreshold = confidenceThreshold;
        this.restTemplate = restTemplate;
        
        // Validate credentials
        if (accessKey == null || accessKey.trim().isEmpty()) {
            throw new IllegalArgumentException("AWS Rekognition access key is required");
        }
        if (secretKey == null || secretKey.trim().isEmpty()) {
            throw new IllegalArgumentException("AWS Rekognition secret key is required");
        }
        
        // Initialize AWS Rekognition client
        try {
            AwsBasicCredentials awsCreds = AwsBasicCredentials.create(accessKey, secretKey);
            this.rekognitionClient = RekognitionClient.builder()
                    .region(Region.of(region))
                    .credentialsProvider(StaticCredentialsProvider.create(awsCreds))
                    .build();
            
            log.info("✅ AWS Rekognition client initialized successfully for region: {}", region);
            log.info("Confidence threshold set to: {}%", confidenceThreshold);
        } catch (Exception e) {
            log.error("❌ Failed to initialize AWS Rekognition client: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to initialize AWS Rekognition client", e);
        }
    }
    
    @PostConstruct
    public void init() {
        log.info("AWS Rekognition Face Verification Client is ready");
    }
    
    @PreDestroy
    public void cleanup() {
        if (rekognitionClient != null) {
            rekognitionClient.close();
            log.info("AWS Rekognition client closed");
        }
    }
    
    @Override
    public FaceVerificationResult verifyFace(Customer customer, byte[] capturedImage) {
        try {
            log.info("🔍 Verifying face for customer ID: {}", customer.getCustomerId());
            
            // 1. Validate inputs
            if (capturedImage == null || capturedImage.length == 0) {
                log.warn("Captured image is empty");
                return FaceVerificationResult.failure("Captured image is empty");
            }
            
            // 2. Load customer's registered face from avatarUrl (Cloudinary)
            String avatarUrl = customer.getAvatarUrl();
            if (avatarUrl == null || avatarUrl.trim().isEmpty()) {
                log.warn("Customer {} has no avatar URL", customer.getCustomerId());
                return FaceVerificationResult.failure("Customer avatar URL is not available");
            }
            
            // 3. Download registered face image from Cloudinary
            log.debug("Downloading registered face image from: {}", avatarUrl);
            byte[] registeredFaceImage = downloadImage(avatarUrl);
            if (registeredFaceImage == null || registeredFaceImage.length == 0) {
                log.error("Failed to download registered face image from: {}", avatarUrl);
                return FaceVerificationResult.failure("Failed to download registered face image");
            }
            
            log.debug("Registered face image size: {} bytes, Captured image size: {} bytes", 
                    registeredFaceImage.length, capturedImage.length);
            
            // 4. Compare faces using AWS Rekognition
            CompareFacesRequest request = CompareFacesRequest.builder()
                    .sourceImage(Image.builder()
                            .bytes(SdkBytes.fromByteArray(registeredFaceImage))
                            .build())
                    .targetImage(Image.builder()
                            .bytes(SdkBytes.fromByteArray(capturedImage))
                            .build())
                    .similarityThreshold(confidenceThreshold)
                    .build();
            
            CompareFacesResponse response = rekognitionClient.compareFaces(request);
            
            // 5. Check if faces match
            List<CompareFacesMatch> faceMatches = response.faceMatches();
            List<ComparedFace> unmatchedSourceFaces = response.unmatchedFaces();
            
            if (faceMatches != null && !faceMatches.isEmpty()) {
                // Get the best match (highest similarity)
                CompareFacesMatch bestMatch = faceMatches.get(0);
                float similarity = bestMatch.similarity();
                double confidenceScore = similarity / 100.0;
                
                log.info("✅ Face match found for customer {} with similarity: {}%", 
                        customer.getCustomerId(), similarity);
                
                return FaceVerificationResult.success(confidenceScore);
            } else {
                log.warn("❌ No face match found for customer {}. Similarity below threshold: {}%", 
                        customer.getCustomerId(), confidenceThreshold);
                log.debug("Unmatched faces count: {}", unmatchedSourceFaces != null ? unmatchedSourceFaces.size() : 0);
                
                return FaceVerificationResult.failure(
                        String.format("Faces do not match. Confidence threshold: %.1f%%", confidenceThreshold));
            }
            
        } catch (InvalidParameterException e) {
            log.error("Invalid parameter in face verification: {}", e.getMessage(), e);
            return FaceVerificationResult.failure("Invalid image format or parameters: " + e.getMessage());
        } catch (ImageTooLargeException e) {
            log.error("Image too large for face verification: {}", e.getMessage(), e);
            return FaceVerificationResult.failure("Image size exceeds maximum limit: " + e.getMessage());
        } catch (InvalidImageFormatException e) {
            log.error("Invalid image format: {}", e.getMessage(), e);
            return FaceVerificationResult.failure("Invalid image format: " + e.getMessage());
        } catch (Exception e) {
            log.error("❌ Error during face verification for customer {}: {}", 
                    customer.getCustomerId(), e.getMessage(), e);
            return FaceVerificationResult.failure("Face verification failed: " + e.getMessage());
        }
    }
    
    /**
     * Download image from URL (Cloudinary or other CDN)
     */
    private byte[] downloadImage(String imageUrl) {
        try {
            // Add timeout and error handling
            return restTemplate.getForObject(imageUrl, byte[].class);
        } catch (Exception e) {
            log.error("Failed to download image from URL: {}", imageUrl, e);
            return null;
        }
    }
}

