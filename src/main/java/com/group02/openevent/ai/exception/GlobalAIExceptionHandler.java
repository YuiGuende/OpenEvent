package com.group02.openevent.ai.exception;

import com.group02.openevent.ai.dto.CustomerResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Global exception handler for AI system
 */
@RestControllerAdvice(basePackages = "com.group02.openevent.ai")
@Slf4j
public class GlobalAIExceptionHandler {

    @ExceptionHandler(AIException.class)
    public ResponseEntity<CustomerResponse> handleAIException(AIException e) {
        log.error("AI Exception [{}]: {}", e.getErrorCode(), e.getMessage(), e);
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new CustomerResponse(e.getUserMessage(), false));
    }
    
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<CustomerResponse> handleIllegalArgument(IllegalArgumentException e) {
        log.warn("Invalid argument: {}", e.getMessage());
        
        return ResponseEntity.badRequest()
                .body(new CustomerResponse("❌ Dữ liệu đầu vào không hợp lệ", false));
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<CustomerResponse> handleGenericException(Exception e) {
        log.error("Unexpected error in AI system: {}", e.getMessage(), e);
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new CustomerResponse("❌ Đã xảy ra lỗi hệ thống. Vui lòng thử lại sau.", false));
    }
}
