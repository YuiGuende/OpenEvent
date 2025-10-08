package com.group02.openevent.dto.department;


import com.group02.openevent.model.order.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderDTO {
    private Long orderId;
    private Long eventId;
    private String eventTitle;
    private String eventImageUrl;
    private String customerName;
    private String customerEmail;
    private String participantName;
    private OrderStatus status;
    private BigDecimal totalAmount;
    private String ticketTypeName;
    private LocalDateTime createdAt;
    
    // Computed fields
    public String getStatusBadgeClass() {
        return switch (status) {
            case CONFIRMED -> "bg-success";
            case PENDING -> "bg-warning";
            case CANCELLED -> "bg-danger";
            case PAID -> "bg-info";
            case EXPIRED -> "bg-secondary";
            case REFUNDED -> "bg-dark";
            default -> "bg-secondary";
        };
    }
    
    public String getFormattedAmount() {
        if (totalAmount == null) return "0đ";
        return String.format("%,.0fđ", totalAmount);
    }
}
