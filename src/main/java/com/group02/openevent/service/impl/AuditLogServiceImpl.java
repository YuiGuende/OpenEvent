package com.group02.openevent.service.impl;


import com.group02.openevent.model.auditLog.AuditLog;
import com.group02.openevent.repository.IAuditLogRepo;
import com.group02.openevent.service.AuditLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class AuditLogServiceImpl implements AuditLogService {
    
    @Autowired
    private IAuditLogRepo auditLogRepo;
    
    @Override
    public AuditLog createAuditLog(String actionType, String entityType, Long entityId, Long actorId, String description) {
        AuditLog auditLog = AuditLog.builder()
                .actionType(actionType)
                .entityType(entityType)
                .entityId(entityId)
                .actorId(actorId)
                .description(description)
                .build();
        return auditLogRepo.save(auditLog);
    }
    
    @Override
    public Page<AuditLog> getAuditLogs(Pageable pageable) {
        return auditLogRepo.findAll(pageable);
    }
    
    @Override
    public Page<AuditLog> getAuditLogsByActionType(String actionType, Pageable pageable) {
        return auditLogRepo.findByActionType(actionType, pageable);
    }
    
    @Override
    public List<AuditLog> getRecentAuditLogs(int days) {
        LocalDateTime date = LocalDateTime.now().minusDays(days);
        return auditLogRepo.findRecentAuditLogs(date);
    }
    
    @Override
    public Long countAuditLogsByActionTypeInDays(String actionType, int days) {
        LocalDateTime date = LocalDateTime.now().minusDays(days);
        return auditLogRepo.countByActionTypeAndDateAfter(actionType, date);
    }
    
    @Override
    public Long countAuditLogsByActionTypeAndDateRange(String actionType, LocalDateTime startDate, LocalDateTime endDate) {
        return auditLogRepo.countByActionTypeAndDateRange(actionType, startDate, endDate);
    }
}
