package com.group02.openevent.service.impl;

import com.group02.openevent.dto.face.FaceVerificationResult;
import com.group02.openevent.model.user.Customer;
import com.group02.openevent.service.FaceVerificationClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Mock implementation of FaceVerificationClient for demo purposes
 * Always returns match = true with confidence = 0.95
 * 
 * TODO: Replace with real face recognition service (AWS Rekognition, Azure Face API, etc.)
 */
@Service
@Slf4j
public class MockFaceVerificationClient implements FaceVerificationClient {
    
    @Override
    public FaceVerificationResult verifyFace(Customer customer, byte[] capturedImage) {
        log.info("Mock face verification for customer ID: {}", customer.getCustomerId());
        log.info("Captured image size: {} bytes", capturedImage != null ? capturedImage.length : 0);
        
        // Mock: Always return match = true for demo
        // In production, this would:
        // 1. Load customer's registered face from avatarUrl
        // 2. Compare with capturedImage using face recognition algorithm
        // 3. Return actual match result with confidence score
        
        return FaceVerificationResult.success(0.95);
    }
}

