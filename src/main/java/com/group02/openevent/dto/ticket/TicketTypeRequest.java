package com.group02.openevent.dto.ticket;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class TicketTypeRequest {
    @NotNull(message = "Event ID is required")
    private Long eventId;

    @NotBlank(message = "Ticket type name is required")
    @Size(max = 100, message = "Name cannot exceed 100 characters")
    private String name;

    @Size(max = 1000, message = "Description cannot exceed 1000 characters")
    private String description;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than 0")
    private BigDecimal price;

    @NotNull(message = "Total quantity is required")
    @Min(value = 1, message = "Total quantity must be at least 1")
    private Integer totalQuantity;

    private LocalDateTime startSaleDate;

    private LocalDateTime endSaleDate;

    // Constructors
    public TicketTypeRequest() {}

    public TicketTypeRequest(Long eventId, String name, String description, BigDecimal price,
                             Integer totalQuantity, LocalDateTime startSaleDate, LocalDateTime endSaleDate) {
        this.eventId = eventId;
        this.name = name;
        this.description = description;
        this.price = price;
        this.totalQuantity = totalQuantity;
        this.startSaleDate = startSaleDate;
        this.endSaleDate = endSaleDate;
    }

    // Getters and Setters
    public Long getEventId() {
        return eventId;
    }

    public void setEventId(Long eventId) {
        this.eventId = eventId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public Integer getTotalQuantity() {
        return totalQuantity;
    }

    public void setTotalQuantity(Integer totalQuantity) {
        this.totalQuantity = totalQuantity;
    }

    public LocalDateTime getStartSaleDate() {
        return startSaleDate;
    }

    public void setStartSaleDate(LocalDateTime startSaleDate) {
        this.startSaleDate = startSaleDate;
    }

    public LocalDateTime getEndSaleDate() {
        return endSaleDate;
    }

    public void setEndSaleDate(LocalDateTime endSaleDate) {
        this.endSaleDate = endSaleDate;
    }

    // Validation method
    public boolean isValidSalePeriod() {
        if (startSaleDate != null && endSaleDate != null) {
            return startSaleDate.isBefore(endSaleDate);
        }
        return true; // Nếu một trong hai null thì không validate
    }

    @Override
    public String toString() {
        return "TicketTypeRequest{" +
                "eventId=" + eventId +
                ", name='" + name + '\'' +
                ", price=" + price +
                ", totalQuantity=" + totalQuantity +
                ", startSaleDate=" + startSaleDate +
                ", endSaleDate=" + endSaleDate +
                '}';
    }
}