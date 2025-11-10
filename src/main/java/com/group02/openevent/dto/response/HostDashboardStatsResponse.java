package com.group02.openevent.dto.response;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HostDashboardStatsResponse {
    
    // Tổng doanh số (Total sales)
    private BigDecimal totalSales;
    
    // Sản phẩm đã bán (Products sold) - tổng số vé đã bán
    private Long productsSold;
    
    // Người tham dự (Attendees) - tổng số người tham dự (tổng quantity)
    private Long attendees;
    
    // Tổng số đơn hàng (Total orders)
    private Long totalOrders;
    
    // Tổng thuế (Total tax) - VAT
    private BigDecimal totalTax;
    
    // Tổng phí (Total fees) - platform fees (nếu có)
    private BigDecimal totalFees;
}

