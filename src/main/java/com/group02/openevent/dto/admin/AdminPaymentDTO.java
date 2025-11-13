package com.group02.openevent.dto.admin;

import com.group02.openevent.model.payment.PaymentStatus;
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
public class AdminPaymentDTO {
    private Long paymentId;
    private Long orderId;
    private Long customerId;
    private String customerName;
    private String customerEmail;
    private BigDecimal amount;
    private String currency;
    private PaymentStatus status;
    private String paymentMethod;
    private String transactionId;
    private String paymentLinkId;
    private LocalDateTime createdAt;
    private LocalDateTime paidAt;
    private LocalDateTime expiredAt;
    private LocalDateTime cancelledAt;
    private String description;
}

