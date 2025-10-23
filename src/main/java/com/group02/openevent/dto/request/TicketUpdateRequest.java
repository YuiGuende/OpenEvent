package com.group02.openevent.dto.request;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TicketUpdateRequest {
    Long ticketTypeId;
    String name;
    String description;
    BigDecimal price;
    Integer totalQuantity;
    Integer soldQuantity;
    LocalDateTime startSaleDate;
    LocalDateTime endSaleDate;
    BigDecimal sale;
    Boolean isNew;
    Boolean isDeleted;
}
