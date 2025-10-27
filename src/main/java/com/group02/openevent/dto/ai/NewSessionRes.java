package com.group02.openevent.dto.ai;

/**
 * DTO for new session response
 */
public record NewSessionRes(
        String sessionId,
        String title
) {}