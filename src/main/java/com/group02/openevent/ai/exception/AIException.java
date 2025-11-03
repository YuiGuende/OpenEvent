package com.group02.openevent.ai.exception;

/**
 * Custom exception for AI-related errors
 */
public class AIException extends RuntimeException {
    private final String errorCode;
    private final String userMessage;
    
    public AIException(String errorCode, String message, String userMessage) {
        super(message);
        this.errorCode = errorCode;
        this.userMessage = userMessage;
    }
    
    public AIException(String errorCode, String message, String userMessage, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.userMessage = userMessage;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
    
    public String getUserMessage() {
        return userMessage;
    }
}

