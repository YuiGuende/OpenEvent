package com.group02.openevent.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import java.util.HashMap;
import java.util.Map;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ResponseStatus;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgumentException(IllegalArgumentException e) {
        log.error("=== GlobalExceptionHandler caught IllegalArgumentException ===");
        log.error("Message: {}", e.getMessage());
        log.error("Stack trace:", e);
        
        Map<String, String> error = new HashMap<>();
        error.put("error", e.getMessage());
        return ResponseEntity
                .badRequest()
                .contentType(MediaType.APPLICATION_JSON)
                .body(error);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleRuntimeException(RuntimeException e) {
        Map<String, String> error = new HashMap<>();
        log.error(e.getMessage(), e);
        error.put("error", "Đã xảy ra lỗi hệ thống. Vui lòng thử lại sau.");
        error.put("message", e.getMessage());
        return ResponseEntity
                .badRequest()
                .contentType(MediaType.APPLICATION_JSON)
                .body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGenericException(Exception e) {
        Map<String, String> error = new HashMap<>();
        error.put("error", "Đã xảy ra lỗi không mong muốn. Vui lòng thử lại sau.");
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .contentType(MediaType.APPLICATION_JSON)
                .body(error);
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN) // Trả về 403
    public ResponseEntity<Object> handleAccessDeniedException(AccessDeniedException ex) {
        log.error("=== GlobalExceptionHandler caught AccessDeniedException ===");
        log.error("Message: {}", ex.getMessage());
        log.error("Stack trace:", ex);
        
        // Bạn có thể dùng DTO lỗi của riêng mình ở đây
        Map<String, String> body = new HashMap<>();
        body.put("error", "Access Denied");
        body.put("message", ex.getMessage()); // Thông báo từ Aspect
        return new ResponseEntity<>(body, HttpStatus.FORBIDDEN);
    }
}
