package com.group02.openevent.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminStatsDTO {
    private Long totalSystemRevenue;
    private Long totalActiveEvents;
    private Long totalNewUsers;
    private Long pendingApprovalRequests;
    private Double revenueChangePercent;
    private List<Map<String, Object>> revenueByMonth;
    private List<Map<String, Object>> eventsByType;
    private List<Map<String, Object>> userRegistrationTrend;
    private Map<String, Object> subscriptionPlanStats;
    private Long approvedEvents;
    private Long rejectedEvents;
    private Long pendingEvents;
}
