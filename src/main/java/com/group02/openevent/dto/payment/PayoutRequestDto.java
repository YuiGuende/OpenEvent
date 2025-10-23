// src/main/java/com/yourapp/dto/wallet/PayoutRequestDto.java
package com.group02.openevent.dto.payment;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PayoutRequestDto {
    private BigDecimal amount;
    private String bankAccountNumber;
    private String bankCode;
    private String otpCode;
}