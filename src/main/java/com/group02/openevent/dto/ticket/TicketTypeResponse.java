package com.group02.openevent.dto.ticket;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

public class TicketTypeResponse {
    private Long ticketTypeId;
    private Long eventId;
    private String eventName;
    private String name;
    private String description;
    private BigDecimal price;
    private Integer totalQuantity;
    private Integer soldQuantity;
    private Integer availableQuantity;
    private LocalDateTime startSaleDate;
    private LocalDateTime endSaleDate;
    private boolean available;
    private boolean salePeriodActive;
    private Map<String, Long> statistics;

    public TicketTypeResponse(Long ticketTypeId, Long eventId, String eventName, String name, String description, BigDecimal price, Integer totalQuantity, Integer soldQuantity, Integer availableQuantity, LocalDateTime startSaleDate, LocalDateTime endSaleDate, boolean available, boolean salePeriodActive) {
        this.ticketTypeId = ticketTypeId;
        this.eventId = eventId;
        this.eventName = eventName;
        this.name = name;
        this.description = description;
        this.price = price;
        this.totalQuantity = totalQuantity;
        this.soldQuantity = soldQuantity;
        this.availableQuantity = availableQuantity;
        this.startSaleDate = startSaleDate;
        this.endSaleDate = endSaleDate;
        this.available = available;
        this.salePeriodActive = salePeriodActive;
    }

    public TicketTypeResponse() {}

    public Long getTicketTypeId() {
        return ticketTypeId;
    }

    public void setTicketTypeId(Long ticketTypeId) {
        this.ticketTypeId = ticketTypeId;
    }

    public Long getEventId() {
        return eventId;
    }

    public void setEventId(Long eventId) {
        this.eventId = eventId;
    }

    public String getEventName() {
        return eventName;
    }

    public void setEventName(String eventName) {
        this.eventName = eventName;
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

    public Integer getSoldQuantity() {
        return soldQuantity;
    }

    public void setSoldQuantity(Integer soldQuantity) {
        this.soldQuantity = soldQuantity;
    }

    public Integer getAvailableQuantity() {
        return availableQuantity;
    }

    public void setAvailableQuantity(Integer availableQuantity) {
        this.availableQuantity = availableQuantity;
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

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    public boolean isSalePeriodActive() {
        return salePeriodActive;
    }

    public void setSalePeriodActive(boolean salePeriodActive) {
        this.salePeriodActive = salePeriodActive;
    }

    public Map<String, Long> getStatistics() {
        return statistics;
    }

    public void setStatistics(Map<String, Long> statistics) {
        this.statistics = statistics;
    }
}
