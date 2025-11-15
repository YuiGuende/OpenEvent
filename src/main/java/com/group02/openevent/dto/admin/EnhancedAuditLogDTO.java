package com.group02.openevent.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnhancedAuditLogDTO {
    private Long auditId;
    private LocalDateTime timestamp;
    private Long userId;
    private String userName;
    private String userEmail;
    private String userRole;
    private String actionType;
    private String entityType;
    private Long entityId;
    private String description;
    private String entityDetails; // Additional info about the entity (e.g., event title)
}

