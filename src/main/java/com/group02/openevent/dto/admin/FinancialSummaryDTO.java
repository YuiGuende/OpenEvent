package com.group02.openevent.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FinancialSummaryDTO {
    // Revenue metrics
    private Long totalRevenue;
    private Long totalOrders;
    private BigDecimal averageOrderValue;
    private Long totalServiceFees;
    private Long totalHostPayouts;
    private Long netProfit;
    private Long pendingPaymentsAmount;
    private Long refundedAmount;
    
    // Growth metrics
    private Double revenueGrowthRate;
    private Double orderGrowthRate;
    
    // Revenue breakdown
    private Map<String, Long> revenueBySource; // Ticket Sales, Subscription, Service Fees
    private Map<String, Long> revenueByEventType; // Music, Festival, Workshop, Competition
    private Map<String, Long> revenueByPaymentMethod; // PayOS, Bank Transfer, etc.
    
    // Order statistics
    private Map<String, Long> ordersByStatus; // PAID, PENDING, CANCELLED, etc.
    private Map<String, Long> paymentsByStatus; // PAID, PENDING, CANCELLED, etc.
    
    // Conversion metrics
    private Double conversionRate;
    private Double refundRate;
}

