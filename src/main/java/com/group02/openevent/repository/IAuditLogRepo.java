package com.group02.openevent.repository;

import com.group02.openevent.model.auditLog.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface IAuditLogRepo extends JpaRepository<AuditLog, Long> {
    
    Page<AuditLog> findByActionType(String actionType, Pageable pageable);
    
    Page<AuditLog> findByEntityType(String entityType, Pageable pageable);
    
    @Query("SELECT a FROM AuditLog a WHERE a.createdAt BETWEEN :startDate AND :endDate ORDER BY a.createdAt DESC")
    List<AuditLog> findByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT COUNT(a) FROM AuditLog a WHERE a.actionType = :actionType AND a.createdAt >= :date")
    Long countByActionTypeAndDateAfter(@Param("actionType") String actionType, @Param("date") LocalDateTime date);
    
    @Query("SELECT a FROM AuditLog a WHERE a.createdAt >= :date ORDER BY a.createdAt DESC")
    List<AuditLog> findRecentAuditLogs(@Param("date") LocalDateTime date);
    
    @Query("SELECT COUNT(a) FROM AuditLog a WHERE a.actionType = :actionType AND a.createdAt >= :startDate AND a.createdAt < :endDate")
    Long countByActionTypeAndDateRange(@Param("actionType") String actionType, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
}
