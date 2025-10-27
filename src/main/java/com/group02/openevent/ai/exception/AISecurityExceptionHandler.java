package com.group02.openevent.ai.exception;

import com.group02.openevent.ai.security.AISecurityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler for AI security exceptions
 */
@RestControllerAdvice(basePackages = "com.group02.openevent.ai")
@RequiredArgsConstructor
@Slf4j
public class AISecurityExceptionHandler {

    private final AISecurityService securityService;

    @ExceptionHandler(AISecurityException.class)
    public ResponseEntity<Map<String, Object>> handleAISecurityException(AISecurityException ex) {
        log.error("AI Security Exception: {} - User: {} - Details: {}", 
            ex.getMessage(), ex.getUserId(), ex.getDetails());
        
        // Log security event
        securityService.logSecurityEvent(ex.getUserId(), "SECURITY_VIOLATION", ex.getDetails());
        
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", "Security Violation");
        errorResponse.put("message", ex.getMessage());
        errorResponse.put("errorCode", ex.getErrorCode());
        errorResponse.put("timestamp", LocalDateTime.now());
        
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgumentException(IllegalArgumentException ex) {
        log.warn("Illegal Argument Exception: {}", ex.getMessage());
        
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", "Invalid Request");
        errorResponse.put("message", ex.getMessage());
        errorResponse.put("timestamp", LocalDateTime.now());
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(RuntimeException ex) {
        log.error("Runtime Exception in AI service: {}", ex.getMessage(), ex);
        
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", "Internal Server Error");
        errorResponse.put("message", "An unexpected error occurred. Please try again later.");
        errorResponse.put("timestamp", LocalDateTime.now());
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
}

