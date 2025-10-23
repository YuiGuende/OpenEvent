package com.group02.openevent.dto.user;

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
public class UserOrderDTO {
    // Order info
    private Long orderId;
    private OrderStatus status;
    private LocalDateTime createdAt;

    // Event info
    private Long eventId;
    private String eventTitle;
    private String eventImageUrl;
    private LocalDateTime eventStartsAt;

    // Ticket info
    private Long ticketTypeId;
    private String ticketTypeName;

    // Price info
    private BigDecimal totalAmount;

    // Helper computed for view classes
    public String getStatusBadgeClass() {
        return switch (status) {
            case PAID -> "status-PAID";
            case PENDING -> "status-PENDING";
            case CANCELLED -> "status-CANCELLED";
            case EXPIRED -> "status-EXPIRED";
            case REFUNDED -> "status-REFUNDED";
        };
    }
}


