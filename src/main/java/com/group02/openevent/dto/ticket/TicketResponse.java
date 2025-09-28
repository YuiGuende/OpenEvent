package com.group02.openevent.dto.ticket;

import com.group02.openevent.model.ticket.TicketStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

public class TicketResponse {
    private Long ticketId;
    private String ticketCode;
    private Long eventId;
    private String eventName;
    private BigDecimal price;
    private TicketStatus status;
    private String participantName;
    private String participantEmail;
    private String participantPhone;
    private String participantOrganization;
    private String notes;
    private String ticketTypeName;
    private LocalDateTime purchaseDate;
    private LocalDateTime updatedAt;
    private Map<String, Long> statistics; // Thống kê tickets

    public TicketResponse() {}

    public TicketResponse(Long ticketId, String ticketCode, Long eventId, String eventName,
                         BigDecimal price, TicketStatus status,
                         String participantName, String participantEmail, String participantPhone,
                         String participantOrganization, String notes, String ticketTypeName,
                         LocalDateTime purchaseDate, LocalDateTime updatedAt) {
        this.ticketId = ticketId;
        this.ticketCode = ticketCode;
        this.eventId = eventId;
        this.eventName = eventName;
        this.price = price;
        this.status = status;
        this.participantName = participantName;
        this.participantEmail = participantEmail;
        this.participantPhone = participantPhone;
        this.participantOrganization = participantOrganization;
        this.notes = notes;
        this.ticketTypeName = ticketTypeName;
        this.purchaseDate = purchaseDate;
        this.updatedAt = updatedAt;
    }

    // Getters and Setters
    public Long getTicketId() {
        return ticketId;
    }

    public void setTicketId(Long ticketId) {
        this.ticketId = ticketId;
    }

    public String getTicketCode() {
        return ticketCode;
    }

    public void setTicketCode(String ticketCode) {
        this.ticketCode = ticketCode;
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


    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public TicketStatus getStatus() {
        return status;
    }

    public void setStatus(TicketStatus status) {
        this.status = status;
    }

    public String getParticipantName() {
        return participantName;
    }

    public void setParticipantName(String participantName) {
        this.participantName = participantName;
    }

    public String getParticipantEmail() {
        return participantEmail;
    }

    public void setParticipantEmail(String participantEmail) {
        this.participantEmail = participantEmail;
    }

    public String getParticipantPhone() {
        return participantPhone;
    }

    public void setParticipantPhone(String participantPhone) {
        this.participantPhone = participantPhone;
    }

    public String getParticipantOrganization() {
        return participantOrganization;
    }

    public void setParticipantOrganization(String participantOrganization) {
        this.participantOrganization = participantOrganization;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getTicketTypeName() {
        return ticketTypeName;
    }

    public void setTicketTypeName(String ticketTypeName) {
        this.ticketTypeName = ticketTypeName;
    }

    public LocalDateTime getPurchaseDate() {
        return purchaseDate;
    }

    public void setPurchaseDate(LocalDateTime purchaseDate) {
        this.purchaseDate = purchaseDate;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Map<String, Long> getStatistics() {
        return statistics;
    }

    public void setStatistics(Map<String, Long> statistics) {
        this.statistics = statistics;
    }
}
