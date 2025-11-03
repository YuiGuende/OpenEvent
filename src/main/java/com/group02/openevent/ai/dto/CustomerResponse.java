package com.group02.openevent.ai.dto;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class CustomerResponse {
    private String message;
    private boolean shouldReload;
    private String timestamp;

    // Constructor cho response đơn giản
    public CustomerResponse(String message, boolean shouldReload) {
        this.message = message;
        this.shouldReload = shouldReload;
        this.timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
    }

    // Constructor cho response với timestamp tùy chỉnh
    public CustomerResponse(String message, boolean shouldReload, String timestamp) {
        this.message = message;
        this.shouldReload = shouldReload;
        this.timestamp = timestamp;
    }

    // Getters
    public String getMessage() {
        return message;
    }

    public boolean isShouldReload() {
        return shouldReload;
    }

    public String getTimestamp() {
        return timestamp;
    }

    // Setters
    public void setMessage(String message) {
        this.message = message;
    }

    public void setShouldReload(boolean shouldReload) {
        this.shouldReload = shouldReload;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }
}

