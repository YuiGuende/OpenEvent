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
        if (sale != null && sale.compareTo(BigDecimal.ZERO) > 0) {
            return String.format("%,.0f", sale);
        }
        return "0";
    }

    public String getSaleEndDate() {
        if (endSaleDate != null) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy");
            return endSaleDate.format(formatter);
        }
        return "N/A";
    }

    public boolean hasSale() {
        return sale != null && sale.compareTo(BigDecimal.ZERO) > 0;
    }

    public int getSalePercentage() {
        if (hasSale() && price.compareTo(BigDecimal.ZERO) > 0) {
            return sale.multiply(BigDecimal.valueOf(100))
                    .divide(price, 0, BigDecimal.ROUND_HALF_UP)
                    .intValue();
        }
        return 0;
    }
}
