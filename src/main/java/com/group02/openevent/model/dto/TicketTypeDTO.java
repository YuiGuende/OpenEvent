package com.group02.openevent.model.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class TicketTypeDTO {
    private Long ticketTypeId;
    private String name;
    private String description;
    private BigDecimal price;
    private Integer availableQuantity;
    private boolean isSoldOut;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime startSaleDate;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime endSaleDate;

    private boolean isSaleActive;

    // ĐÃ XÓA TRƯỜNG "private List<TicketTypeDTO> ticketTypes;" BỊ THỪA Ở ĐÂY

    // Constructors...
    public TicketTypeDTO() {}

    public TicketTypeDTO(Long ticketTypeId, String name, String description, BigDecimal price, Integer availableQuantity, boolean isSoldOut, LocalDateTime startSaleDate, LocalDateTime endSaleDate, boolean isSaleActive) {
        this.ticketTypeId = ticketTypeId;
        this.name = name;
        this.description = description;
        this.price = price;
        this.availableQuantity = availableQuantity;
        this.isSoldOut = isSoldOut;
        this.startSaleDate = startSaleDate;
        this.endSaleDate = endSaleDate;
        this.isSaleActive = isSaleActive;
    }

    public void setSaleActive(boolean saleActive) {
        isSaleActive = saleActive;
    }

    // Getters and Setters...
    // (Xóa getTicketTypes() và setTicketTypes())

    // ... (tất cả getters và setters khác giữ nguyên)
    public Long getTicketTypeId() { return ticketTypeId; }
    public void setTicketTypeId(Long ticketTypeId) { this.ticketTypeId = ticketTypeId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public Integer getAvailableQuantity() { return availableQuantity; }
    public void setAvailableQuantity(Integer availableQuantity) { this.availableQuantity = availableQuantity; }
    public boolean isSoldOut() { return isSoldOut; }
    public void setSoldOut(boolean soldOut) { isSoldOut = soldOut; }
    public LocalDateTime getStartSaleDate() { return startSaleDate; }
    public void setStartSaleDate(LocalDateTime startSaleDate) { this.startSaleDate = startSaleDate; }
    public LocalDateTime getEndSaleDate() { return endSaleDate; }
    public void setEndSaleDate(LocalDateTime endSaleDate) { this.endSaleDate = endSaleDate; }
    public boolean isSaleActive() { return isSaleActive; }
}
