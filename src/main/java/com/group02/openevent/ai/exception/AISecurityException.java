package com.group02.openevent.ai.exception;

import lombok.Getter;

/**
 * Exception for AI security violations
 */
@Getter
public class AISecurityException extends RuntimeException {
    
    private final String errorCode;
    private final String userId;
    private final String details;

    public AISecurityException(String message, String errorCode, String userId, String details) {
        super(message);
        this.errorCode = errorCode;
        this.userId = userId;
        this.details = details;
    }

    public AISecurityException(String message, String errorCode) {
        this(message, errorCode, null, null);
    }

    public AISecurityException(String message) {
        this(message, "AI_SECURITY_ERROR", null, null);
    }
}

