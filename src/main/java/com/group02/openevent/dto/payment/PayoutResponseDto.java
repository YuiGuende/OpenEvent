package com.group02.openevent.dto.payment;

import com.group02.openevent.model.payment.PayoutStatus;
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
public class PayoutResponseDto {
    private Long id;
    private BigDecimal amount;
    private String bankAccountNumber;
    private String bankCode;
    private String payosOrderCode;
    private String payosTransactionId;
    private PayoutStatus status;
    private BigDecimal transactionFee;
    private LocalDateTime requestedAt;
    private LocalDateTime processedAt;
}

