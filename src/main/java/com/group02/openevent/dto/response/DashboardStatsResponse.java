package com.group02.openevent.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardStatsResponse {
    
    // KPI Cards Data
    private BigDecimal totalRevenue;
    private long totalTicketsSold;
    private long totalAttendees;
    private long totalCheckIn;
    private long totalRefunded;
    private long totalQuantity;
    private long unsoldTickets;
    private long vouchersUsed; // Thêm voucher data
    
    // Percentage Rates
    private double checkInRate;
    private double refundRate;
    private double unsoldRate;
    
    // Chart Data
    private List<TicketTypeStats> ticketTypeStats;
    private List<RevenueByType> revenueByType;
    private List<DailyStats> dailyStats; // Thêm dữ liệu theo ngày
    
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TicketTypeStats {
        private Long ticketTypeId;
        private String name;
        private BigDecimal price;
        private long totalQuantity;
        private long soldQuantity;
        private long unsoldQuantity;
        private long checkInCount;
        private double checkInRate;
    }
    
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RevenueByType {
        private String ticketTypeName;
        private BigDecimal revenue;
        private double percentage;
    }
    
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DailyStats {
        private String date;
        private BigDecimal revenue;
        private long ordersCount;
        private long ticketsSold;
    }
}
