package com.group02.openevent.dto.admin;

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
public class AdminOrderDTO {
    private Long orderId;
    private Long customerId;
    private String customerName;
    private String customerEmail;
    private Long eventId;
    private String eventTitle;
    private Long ticketTypeId;
    private String ticketTypeName;
    private Integer quantity;
    private BigDecimal totalAmount;
    private OrderStatus status;
    private String paymentStatus;
    private LocalDateTime createdAt;
    private LocalDateTime paidAt;
    private String participantName;
    private String participantEmail;
    private String participantPhone;
}

