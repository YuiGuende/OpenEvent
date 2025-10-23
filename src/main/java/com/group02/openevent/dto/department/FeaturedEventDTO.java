package com.group02.openevent.dto.department;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeaturedEventDTO {
    private Long eventId;
    private String title;
    private String imageUrl;
    private Long ticketsSold;
    private BigDecimal totalRevenue;
    private Integer rank;
    
    public String getFormattedRevenue() {
        if (totalRevenue == null) return "0đ";
        return String.format("%,.0fđ", totalRevenue);
    }
}