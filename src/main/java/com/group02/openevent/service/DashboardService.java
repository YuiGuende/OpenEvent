package com.group02.openevent.service;

import com.group02.openevent.dto.response.DashboardStatsResponse;

public interface DashboardService {
    DashboardStatsResponse getEventDashboardStats(Long eventId);
}
