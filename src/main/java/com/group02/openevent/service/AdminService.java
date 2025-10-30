package com.group02.openevent.service;

import com.group02.openevent.dto.admin.AdminStatsDTO;

import java.time.LocalDate;

public interface AdminService {
    AdminStatsDTO getAdminDashboardStats();

    AdminStatsDTO getAdminDashboardStatsByPeriod(String period, LocalDate startDate, LocalDate endDate);
}
