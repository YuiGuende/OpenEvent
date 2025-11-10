package com.group02.openevent.dto.ticket;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketTypeDTO {
    private Long ticketTypeId;
    private Long eventId;
    private String eventTitle;
    private String eventImageUrl;
    private String name;
    private String description;
    private BigDecimal price;
    private BigDecimal sale;
    private BigDecimal finalPrice;
    private Integer totalQuantity;
    private Integer soldQuantity;
    private Integer availableQuantity;
    private LocalDateTime startSaleDate;
    private LocalDateTime endSaleDate;
    private boolean isAvailable;
    private boolean isSaleActive;
    private boolean isSoldOut;
    private boolean saleNotStarted;
    private String saleStartCountdownText;
    private boolean saleOverdue;
    // Computed fields for display
    public String getFormattedPrice() {
        return String.format("%,.0f", price);
    }

    public String getFormattedFinalPrice() {
        return String.format("%,.0f", finalPrice);
    }

    public String getFormattedSale() {
        // NOTE: sale field stores PERCENTAGE (0-100)
        if (sale != null && sale.compareTo(BigDecimal.ZERO) > 0) {
            return String.format("%.0f%%", sale.doubleValue());
        }
        return "0%";
    }

    public String getSaleEndDate() {
        if (endSaleDate != null) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy");
            return endSaleDate.format(formatter);
        }
        return "N/A";
    }

    public boolean hasSale() {
        // Sale exists if sale amount > 0 and price > 0
        // Note: Sale period check (isSaleActive) is handled separately for display logic
        // If sale > 0, we show it regardless of sale period status
        return sale != null 
                && sale.compareTo(BigDecimal.ZERO) > 0 
                && price != null 
                && price.compareTo(BigDecimal.ZERO) > 0;
    }

    public int getSalePercentage() {
        // NOTE: 'sale' field in DB stores PERCENTAGE (0-100), not absolute amount
        // So we can directly return it as integer after rounding
        if (sale != null && sale.compareTo(BigDecimal.ZERO) > 0) {
            try {
                // Sale is already a percentage, just round to nearest integer
                return sale.setScale(0, BigDecimal.ROUND_HALF_UP).intValue();
            } catch (Exception e) {
                // Fallback: if calculation fails, return 0
                return 0;
            }
        }
        return 0;
    }
}
