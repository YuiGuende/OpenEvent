package com.group02.openevent.dto.department;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DepartmentStatsDTO {
    // Overview statistics
    private long totalEvents;
    private long pendingRequests;
    private long ongoingEvents;
    private long totalParticipants;
    private Double revenueChangePercent;
    private Double cancellationRateChangePercent;
    private BigDecimal totalRevenue;
    private BigDecimal avgOrderValue;
    private double cancellationRate;
    private long uniqueCustomers;
    private long totalOrders;

    // Event statistics by status
    private long publicEvents;
    private long draftEvents;
    private long finishedEvents;
    private long cancelledEvents;

    // Event statistics by type
    private long musicEvents;
    private long festivalEvents;
    private long workshopEvents;
    private long competitionEvents;

    // Article statistics
    private long totalArticles;
    private long publishedArticles;
    private long draftArticles;

    // Time-based statistics (for charts)
    private List<MonthlyEventStats> monthlyStats;

    // Event type distribution (for pie chart)
    private Map<String, Long> eventTypeDistribution;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MonthlyEventStats {
        private String month; // Format: "T1", "T2", etc.
        private long eventCount;
        private long participantCount;
    }
}
