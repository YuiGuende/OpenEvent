package com.group02.openevent.service;

import com.group02.openevent.dto.response.DashboardStatsResponse;
import com.group02.openevent.dto.response.HostDashboardStatsResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface DashboardService {
    DashboardStatsResponse getEventDashboardStats(Long eventId);
    HostDashboardStatsResponse getHostDashboardStats(Long hostId);
    Page<com.group02.openevent.model.order.Order> getHostOrders(Long hostId, Pageable pageable);
    Page<com.group02.openevent.model.order.Order> getHostPaidOrders(Long hostId, Pageable pageable);
}
