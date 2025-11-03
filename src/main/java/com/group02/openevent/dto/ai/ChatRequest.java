package com.group02.openevent.dto.ai;

/**
 * DTO for chat request
 */
public record ChatRequest(
        String message,
        Long userId,
        String sessionId
) {}