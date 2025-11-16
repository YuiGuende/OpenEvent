package com.group02.openevent.dto.face;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Result of face verification process
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FaceVerificationResult {
    private boolean match;  // Whether the face matches
    private double confidence;  // Confidence score (0.0 - 1.0)
    private String message;  // Optional message about the result
    
    public static FaceVerificationResult success(double confidence) {
        return new FaceVerificationResult(true, confidence, "Face matched successfully");
    }
    
    public static FaceVerificationResult failure(String message) {
        return new FaceVerificationResult(false, 0.0, message);
    }
}

