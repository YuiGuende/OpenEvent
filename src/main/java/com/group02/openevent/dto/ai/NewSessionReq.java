package com.group02.openevent.dto.ai;

/**
 * DTO for creating new session request
 */
public record NewSessionReq(
        Long userId,
        String title
) {}