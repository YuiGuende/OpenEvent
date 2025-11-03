package com.group02.openevent.dto.ai;

import java.time.LocalDateTime;

/**
 * DTO for session item
 */
public record SessionItem(
        String sessionId,
        String title,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}