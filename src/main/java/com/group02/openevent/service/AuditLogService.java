package com.group02.openevent.service;

import com.group02.openevent.model.auditLog.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.time.LocalDateTime;
import java.util.List;

public interface AuditLogService {
    AuditLog createAuditLog(String actionType, String entityType, Long entityId, Long actorId, String description);
    Page<AuditLog> getAuditLogs(Pageable pageable);
    Page<AuditLog> getAuditLogsByActionType(String actionType, Pageable pageable);
    List<AuditLog> getRecentAuditLogs(int days);
    Long countAuditLogsByActionTypeInDays(String actionType, int days);
    Long countAuditLogsByActionTypeAndDateRange(String actionType, LocalDateTime startDate, LocalDateTime endDate);
}
