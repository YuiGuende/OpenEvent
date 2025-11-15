package com.group02.openevent.service;

import com.group02.openevent.dto.admin.*;
import com.group02.openevent.model.enums.EventStatus;
import com.group02.openevent.model.enums.EventType;
import com.group02.openevent.model.order.OrderStatus;
import com.group02.openevent.model.payment.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface AdminService {
    AdminStatsDTO getAdminDashboardStats();

    AdminStatsDTO getAdminDashboardStatsByPeriod(String period, LocalDate startDate, LocalDate endDate);
    
    // Financial Reports methods
    Page<AdminOrderDTO> getOrders(OrderStatus status, String search, LocalDate fromDate, LocalDate toDate, Pageable pageable);
    
    Page<AdminPaymentDTO> getPayments(PaymentStatus status, String search, LocalDate fromDate, LocalDate toDate, Pageable pageable);
    
    FinancialSummaryDTO getFinancialSummary(LocalDate fromDate, LocalDate toDate);
    
    List<AdminOrderDTO> getAllOrdersForExport(OrderStatus status, String search, LocalDate fromDate, LocalDate toDate);
    
    List<AdminPaymentDTO> getAllPaymentsForExport(PaymentStatus status, String search, LocalDate fromDate, LocalDate toDate);
    
    // Event Operations methods
    Page<PendingApprovalDTO> getPendingApprovals(Pageable pageable);
    
    Page<EventStatusDTO> getEventsByStatus(EventStatus status, EventType eventType, Long departmentId, String search, LocalDate fromDate, LocalDate toDate, Pageable pageable);
    
    Page<UpcomingEventDTO> getUpcomingEvents(int days, Pageable pageable);
    
    List<DepartmentEventStatsDTO> getDepartmentEventStatistics();
    
    Page<AttendanceStatsDTO> getAttendanceStatistics(EventType eventType, LocalDate fromDate, LocalDate toDate, Pageable pageable);
    
    List<VenueConflictDTO> getVenueConflicts();
    
    Page<PointsTrackingDTO> getPointsTracking(EventType eventType, LocalDate fromDate, LocalDate toDate, Pageable pageable);
    
    Page<SpeakerStatsDTO> getSpeakerStatistics(Pageable pageable);
    
    Map<String, Object> getEventPerformanceMetrics(LocalDate fromDate, LocalDate toDate);
    
    boolean bulkApproveEvents(List<Long> eventIds);
    
    boolean bulkRejectEvents(List<Long> eventIds, String reason);
    
    boolean bulkUpdateEventStatus(List<Long> eventIds, EventStatus status);
    
    // User Activity methods
    UserStatisticsDTO getUserStatistics();
    
    Page<UserListDTO> getUsers(String role, String search, LocalDate fromDate, LocalDate toDate, Pageable pageable);
    
    Page<EnhancedAuditLogDTO> getEnhancedAuditLogs(String actionType, String entityType, Long userId, String search, LocalDate fromDate, LocalDate toDate, Pageable pageable);
    
    List<EnhancedAuditLogDTO> getAllAuditLogsForExport(String actionType, String entityType, Long userId, String search, LocalDate fromDate, LocalDate toDate);
}
