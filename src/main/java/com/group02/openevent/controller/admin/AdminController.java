package com.group02.openevent.controller.admin;


import com.group02.openevent.dto.admin.AdminStatsDTO;
import com.group02.openevent.model.auditLog.AuditLog;
import com.group02.openevent.service.AdminService;
import com.group02.openevent.service.AuditLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

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
}
