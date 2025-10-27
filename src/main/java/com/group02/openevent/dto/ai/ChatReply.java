package com.group02.openevent.dto.ai;

import java.time.LocalDateTime;

/**
 * DTO for chat reply
 */
public record ChatReply(
        String message,
        Boolean shouldReload,
        LocalDateTime timestamp
) {}