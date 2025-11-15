package com.group02.openevent.controller.admin;


import com.group02.openevent.dto.admin.*;
import com.group02.openevent.model.auditLog.AuditLog;
import com.group02.openevent.model.enums.EventStatus;
import com.group02.openevent.model.enums.EventType;
import com.group02.openevent.model.order.OrderStatus;
import com.group02.openevent.model.payment.PaymentStatus;
import com.group02.openevent.service.AdminService;
import com.group02.openevent.service.AuditLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Slf4j
@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private AdminService adminService;
    @Autowired
    private AuditLogService auditLogService;

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        AdminStatsDTO stats = adminService.getAdminDashboardStats();
        model.addAttribute("stats", stats);
        return "admin/dashboard";
    }

    @GetMapping("/api/stats")
    @ResponseBody
    public ResponseEntity<AdminStatsDTO> getAdminStats(
            @RequestParam(value = "period", defaultValue = "month") String period,
            @RequestParam(value = "startDate", required = false) LocalDate startDate,
            @RequestParam(value = "endDate", required = false) LocalDate endDate) {
        AdminStatsDTO stats = adminService.getAdminDashboardStatsByPeriod(period, startDate, endDate);
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/api/audit-logs")
    @ResponseBody
    public ResponseEntity<List<AuditLog>> getAuditLogs() {
        List<AuditLog> logs = auditLogService.getRecentAuditLogs(30);
        return ResponseEntity.ok(logs);
    }
    
    // Financial Reports APIs
    @GetMapping("/api/orders")
    @ResponseBody
    public ResponseEntity<Page<AdminOrderDTO>> getOrders(
            @RequestParam(value = "status", required = false) OrderStatus status,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "fromDate", required = false) LocalDate fromDate,
            @RequestParam(value = "toDate", required = false) LocalDate toDate,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @RequestParam(value = "sort", defaultValue = "createdAt") String sort,
            @RequestParam(value = "direction", defaultValue = "DESC") String direction) {
        Sort.Direction sortDirection = direction.equalsIgnoreCase("ASC") ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sort));
        Page<AdminOrderDTO> orders = adminService.getOrders(status, search, fromDate, toDate, pageable);
        return ResponseEntity.ok(orders);
    }
    
    @GetMapping("/api/payments")
    @ResponseBody
    public ResponseEntity<Page<AdminPaymentDTO>> getPayments(
            @RequestParam(value = "status", required = false) PaymentStatus status,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "fromDate", required = false) LocalDate fromDate,
            @RequestParam(value = "toDate", required = false) LocalDate toDate,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @RequestParam(value = "sort", defaultValue = "createdAt") String sort,
            @RequestParam(value = "direction", defaultValue = "DESC") String direction) {
        Sort.Direction sortDirection = direction.equalsIgnoreCase("ASC") ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sort));
        Page<AdminPaymentDTO> payments = adminService.getPayments(status, search, fromDate, toDate, pageable);
        return ResponseEntity.ok(payments);
    }
    
    @GetMapping("/api/financial-summary")
    @ResponseBody
    public ResponseEntity<FinancialSummaryDTO> getFinancialSummary(
            @RequestParam(value = "fromDate", required = false) LocalDate fromDate,
            @RequestParam(value = "toDate", required = false) LocalDate toDate) {
        FinancialSummaryDTO summary = adminService.getFinancialSummary(fromDate, toDate);
        return ResponseEntity.ok(summary);
    }
    
    @GetMapping("/api/orders/export")
    @ResponseBody
    public ResponseEntity<List<AdminOrderDTO>> exportOrders(
            @RequestParam(value = "status", required = false) OrderStatus status,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "fromDate", required = false) LocalDate fromDate,
            @RequestParam(value = "toDate", required = false) LocalDate toDate) {
        List<AdminOrderDTO> orders = adminService.getAllOrdersForExport(status, search, fromDate, toDate);
        return ResponseEntity.ok(orders);
    }
    
    @GetMapping("/api/payments/export")
    @ResponseBody
    public ResponseEntity<List<AdminPaymentDTO>> exportPayments(
            @RequestParam(value = "status", required = false) PaymentStatus status,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "fromDate", required = false) LocalDate fromDate,
            @RequestParam(value = "toDate", required = false) LocalDate toDate) {
        List<AdminPaymentDTO> payments = adminService.getAllPaymentsForExport(status, search, fromDate, toDate);
        return ResponseEntity.ok(payments);
    }
    
    // Event Operations APIs
    @GetMapping("/api/pending-approvals")
    @ResponseBody
    public ResponseEntity<Page<PendingApprovalDTO>> getPendingApprovals(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        log.info("API called: GET /admin/api/pending-approvals?page={}&size={}", page, size);
        Pageable pageable = PageRequest.of(page, size);
        Page<PendingApprovalDTO> approvals = adminService.getPendingApprovals(pageable);
        log.info("API response: returning {} items (total: {})", approvals.getContent().size(), approvals.getTotalElements());
        return ResponseEntity.ok(approvals);
    }
    
    @GetMapping("/api/events/status")
    @ResponseBody
    public ResponseEntity<Page<EventStatusDTO>> getEventsByStatus(
            @RequestParam(value = "status", required = false) EventStatus status,
            @RequestParam(value = "eventType", required = false) EventType eventType,
            @RequestParam(value = "departmentId", required = false) Long departmentId,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "fromDate", required = false) LocalDate fromDate,
            @RequestParam(value = "toDate", required = false) LocalDate toDate,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @RequestParam(value = "sort", defaultValue = "createdAt") String sort,
            @RequestParam(value = "direction", defaultValue = "DESC") String direction) {
        log.info("API called: GET /admin/api/events/status?status={}&eventType={}&departmentId={}&search={}&fromDate={}&toDate={}&page={}&size={}", 
            status, eventType, departmentId, search, fromDate, toDate, page, size);
        Sort.Direction sortDirection = direction.equalsIgnoreCase("ASC") ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sort));
        Page<EventStatusDTO> events = adminService.getEventsByStatus(status, eventType, departmentId, search, fromDate, toDate, pageable);
        log.info("API response: returning {} items (total: {})", events.getContent().size(), events.getTotalElements());
        return ResponseEntity.ok(events);
    }
    
    @GetMapping("/api/events/upcoming")
    @ResponseBody
    public ResponseEntity<Page<UpcomingEventDTO>> getUpcomingEvents(
            @RequestParam(value = "days", defaultValue = "30") int days,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        log.info("API called: GET /admin/api/events/upcoming?days={}&page={}&size={}", days, page, size);
        Pageable pageable = PageRequest.of(page, size);
        Page<UpcomingEventDTO> events = adminService.getUpcomingEvents(days, pageable);
        log.info("API response: returning {} items (total: {})", events.getContent().size(), events.getTotalElements());
        return ResponseEntity.ok(events);
    }
    
    @GetMapping("/api/departments/stats")
    @ResponseBody
    public ResponseEntity<List<DepartmentEventStatsDTO>> getDepartmentEventStatistics() {
        log.info("API called: GET /admin/api/departments/stats");
        List<DepartmentEventStatsDTO> stats = adminService.getDepartmentEventStatistics();
        log.info("API response: returning {} department statistics", stats.size());
        return ResponseEntity.ok(stats);
    }
    
    @GetMapping("/api/attendance/stats")
    @ResponseBody
    public ResponseEntity<Page<AttendanceStatsDTO>> getAttendanceStatistics(
            @RequestParam(value = "eventType", required = false) EventType eventType,
            @RequestParam(value = "fromDate", required = false) LocalDate fromDate,
            @RequestParam(value = "toDate", required = false) LocalDate toDate,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        log.info("API called: GET /admin/api/attendance/stats?eventType={}&fromDate={}&toDate={}&page={}&size={}", 
            eventType, fromDate, toDate, page, size);
        Pageable pageable = PageRequest.of(page, size);
        Page<AttendanceStatsDTO> stats = adminService.getAttendanceStatistics(eventType, fromDate, toDate, pageable);
        log.info("API response: returning {} items (total: {})", stats.getContent().size(), stats.getTotalElements());
        return ResponseEntity.ok(stats);
    }
    
    @GetMapping("/api/venue/conflicts")
    @ResponseBody
    public ResponseEntity<List<VenueConflictDTO>> getVenueConflicts() {
        log.info("API called: GET /admin/api/venue/conflicts");
        List<VenueConflictDTO> conflicts = adminService.getVenueConflicts();
        log.info("API response: returning {} conflicts", conflicts.size());
        return ResponseEntity.ok(conflicts);
    }
    
    @GetMapping("/api/points/tracking")
    @ResponseBody
    public ResponseEntity<Page<PointsTrackingDTO>> getPointsTracking(
            @RequestParam(value = "eventType", required = false) EventType eventType,
            @RequestParam(value = "fromDate", required = false) LocalDate fromDate,
            @RequestParam(value = "toDate", required = false) LocalDate toDate,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        log.info("API called: GET /admin/api/points/tracking?eventType={}&fromDate={}&toDate={}&page={}&size={}", 
            eventType, fromDate, toDate, page, size);
        Pageable pageable = PageRequest.of(page, size);
        Page<PointsTrackingDTO> tracking = adminService.getPointsTracking(eventType, fromDate, toDate, pageable);
        log.info("API response: returning {} items (total: {})", tracking.getContent().size(), tracking.getTotalElements());
        return ResponseEntity.ok(tracking);
    }
    
    @GetMapping("/api/speakers/stats")
    @ResponseBody
    public ResponseEntity<Page<SpeakerStatsDTO>> getSpeakerStatistics(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        log.info("API called: GET /admin/api/speakers/stats?page={}&size={}", page, size);
        Pageable pageable = PageRequest.of(page, size);
        Page<SpeakerStatsDTO> stats = adminService.getSpeakerStatistics(pageable);
        log.info("API response: returning {} items (total: {})", stats.getContent().size(), stats.getTotalElements());
        return ResponseEntity.ok(stats);
    }
    
    @GetMapping("/api/events/performance-metrics")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getEventPerformanceMetrics(
            @RequestParam(value = "fromDate", required = false) LocalDate fromDate,
            @RequestParam(value = "toDate", required = false) LocalDate toDate) {
        log.info("API called: GET /admin/api/events/performance-metrics?fromDate={}&toDate={}", fromDate, toDate);
        Map<String, Object> metrics = adminService.getEventPerformanceMetrics(fromDate, toDate);
        log.info("API response: returning metrics with {} keys", metrics.size());
        return ResponseEntity.ok(metrics);
    }
    
    @PostMapping("/api/events/bulk-approve")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> bulkApproveEvents(@RequestBody List<Long> eventIds) {
        boolean success = adminService.bulkApproveEvents(eventIds);
        return ResponseEntity.ok(Map.of("success", success));
    }
    
    @PostMapping("/api/events/bulk-reject")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> bulkRejectEvents(
            @RequestBody Map<String, Object> request) {
        @SuppressWarnings("unchecked")
        List<Long> eventIds = (List<Long>) request.get("eventIds");
        String reason = (String) request.get("reason");
        boolean success = adminService.bulkRejectEvents(eventIds, reason);
        return ResponseEntity.ok(Map.of("success", success));
    }
    
    @PostMapping("/api/events/bulk-update-status")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> bulkUpdateEventStatus(
            @RequestBody Map<String, Object> request) {
        @SuppressWarnings("unchecked")
        List<Long> eventIds = (List<Long>) request.get("eventIds");
        EventStatus status = EventStatus.valueOf((String) request.get("status"));
        boolean success = adminService.bulkUpdateEventStatus(eventIds, status);
        return ResponseEntity.ok(Map.of("success", success));
    }
    
    // ========== User Activity APIs ==========
    
    @GetMapping("/api/users/statistics")
    @ResponseBody
    public ResponseEntity<UserStatisticsDTO> getUserStatistics() {
        log.info("========== API CALLED: GET /admin/api/users/statistics ==========");
        try {
            UserStatisticsDTO stats = adminService.getUserStatistics();
            log.info("API response: returning user statistics - Total Users: {}, Active Users: {}", 
                stats.getTotalUsers(), stats.getActiveUsers());
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("Error in getUserStatistics API: ", e);
            throw e;
        }
    }
    
    @GetMapping("/api/users")
    @ResponseBody
    public ResponseEntity<Page<UserListDTO>> getUsers(
            @RequestParam(value = "role", required = false) String role,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "fromDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(value = "toDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @RequestParam(value = "sort", defaultValue = "lastActivityDate") String sort,
            @RequestParam(value = "direction", defaultValue = "DESC") String direction) {
        log.info("========== API CALLED: GET /admin/api/users ==========");
        log.info("Parameters: role={}, search={}, fromDate={}, toDate={}, page={}, size={}", 
            role, search, fromDate, toDate, page, size);
        try {
            Sort.Direction sortDirection = direction.equalsIgnoreCase("ASC") ? Sort.Direction.ASC : Sort.Direction.DESC;
            Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sort));
            Page<UserListDTO> users = adminService.getUsers(role, search, fromDate, toDate, pageable);
            log.info("API response: returning {} users (total: {})", users.getContent().size(), users.getTotalElements());
            return ResponseEntity.ok(users);
        } catch (Exception e) {
            log.error("Error in getUsers API: ", e);
            throw e;
        }
    }
    
    @GetMapping("/api/audit-logs/enhanced")
    @ResponseBody
    public ResponseEntity<Page<EnhancedAuditLogDTO>> getEnhancedAuditLogs(
            @RequestParam(value = "actionType", required = false) String actionType,
            @RequestParam(value = "entityType", required = false) String entityType,
            @RequestParam(value = "userId", required = false) Long userId,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "fromDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(value = "toDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @RequestParam(value = "sort", defaultValue = "timestamp") String sort,
            @RequestParam(value = "direction", defaultValue = "DESC") String direction) {
        log.info("========== API CALLED: GET /admin/api/audit-logs/enhanced ==========");
        log.info("Parameters: actionType={}, entityType={}, userId={}, search={}, fromDate={}, toDate={}, page={}, size={}", 
            actionType, entityType, userId, search, fromDate, toDate, page, size);
        try {
            Sort.Direction sortDirection = direction.equalsIgnoreCase("ASC") ? Sort.Direction.ASC : Sort.Direction.DESC;
            Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sort));
            Page<EnhancedAuditLogDTO> logs = adminService.getEnhancedAuditLogs(actionType, entityType, userId, search, fromDate, toDate, pageable);
            log.info("API response: returning {} audit logs (total: {})", logs.getContent().size(), logs.getTotalElements());
            return ResponseEntity.ok(logs);
        } catch (Exception e) {
            log.error("Error in getEnhancedAuditLogs API: ", e);
            throw e;
        }
    }
    
    @GetMapping("/api/audit-logs/export")
    @ResponseBody
    public ResponseEntity<List<EnhancedAuditLogDTO>> exportAuditLogs(
            @RequestParam(value = "actionType", required = false) String actionType,
            @RequestParam(value = "entityType", required = false) String entityType,
            @RequestParam(value = "userId", required = false) Long userId,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "fromDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(value = "toDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        log.info("API called: GET /admin/api/audit-logs/export?actionType={}&entityType={}&userId={}&search={}&fromDate={}&toDate={}", 
            actionType, entityType, userId, search, fromDate, toDate);
        List<EnhancedAuditLogDTO> logs = adminService.getAllAuditLogsForExport(actionType, entityType, userId, search, fromDate, toDate);
        return ResponseEntity.ok(logs);
    }
}
