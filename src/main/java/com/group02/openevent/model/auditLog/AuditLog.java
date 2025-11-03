package com.group02.openevent.model.auditLog;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "audit_log")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "audit_id")
    private Long auditId;

    @Column(name = "action_type", nullable = false, length = 50)
    private String actionType; // NOTIFICATION_CREATED, REQUEST_CREATED, REQUEST_APPROVED, ARTICLE_CREATED, etc.

    @Column(name = "entity_type", nullable = false, length = 50)
    private String entityType; // NOTIFICATION, REQUEST, ARTICLE, EVENT, etc.

    @Column(name = "entity_id")
    private Long entityId;

    @Column(name = "actor_id")
    private Long actorId; // User/Department who performed the action

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}