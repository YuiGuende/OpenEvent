package com.group02.openevent.service;

import com.group02.openevent.dto.face.FaceVerificationResult;
import com.group02.openevent.model.user.Customer;

/**
 * Interface for face verification service
 * Can be implemented with various face recognition APIs (AWS Rekognition, Azure Face API, etc.)
 */
public interface FaceVerificationClient {
    
    /**
     * Verify if the captured image matches the customer's registered face
     * 
     * @param customer The customer to verify against
     * @param capturedImage The image captured from camera (byte array)
     * @return FaceVerificationResult containing match status and confidence
     */
    FaceVerificationResult verifyFace(Customer customer, byte[] capturedImage);
}

