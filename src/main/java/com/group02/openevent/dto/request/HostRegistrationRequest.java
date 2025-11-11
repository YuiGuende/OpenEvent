package com.group02.openevent.dto.request;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class HostRegistrationRequest {
    private String hostName; // Tên host (bắt buộc hoặc optional)
    private Long organizationId; // Optional - có thể null
    private BigDecimal hostDiscountPercent; // Optional - có thể null, default 0
    private String description; // Optional - có thể null
}

