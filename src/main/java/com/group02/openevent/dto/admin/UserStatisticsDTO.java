package com.group02.openevent.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserStatisticsDTO {
    private Long totalUsers;
    private Long totalCustomers;
    private Long totalHosts;
    private Long activeUsers; // Active in last 30 days
    private Long newUsersThisMonth;
    private Double retentionRate; // Percentage
    private Long dailyActiveUsers; // DAU
    private Long weeklyActiveUsers; // WAU
    private Long monthlyActiveUsers; // MAU
    private Double averageEventsPerUser;
    private Double averageTicketsPerUser;
    private BigDecimal averageSpendingPerUser;
    private Long totalFeedbackCount;
}

