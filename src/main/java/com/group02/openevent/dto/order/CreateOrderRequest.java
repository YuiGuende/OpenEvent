package com.group02.openevent.dto.order;

import java.math.BigDecimal;

public class CreateOrderRequest {
    private Long eventId;
    private BigDecimal amount;
    private String description;
    private String participantName;
    private String participantEmail;
    private String participantPhone;
    private String participantOrganization;
    private String notes;

    public CreateOrderRequest() {}

    public CreateOrderRequest(Long eventId, BigDecimal amount, String description, String participantName, 
                             String participantEmail, String participantPhone, 
                             String participantOrganization, String notes) {
        this.eventId = eventId;
        this.amount = amount;
        this.description = description;
        this.participantName = participantName;
        this.participantEmail = participantEmail;
        this.participantPhone = participantPhone;
        this.participantOrganization = participantOrganization;
        this.notes = notes;
    }

    // Getters and Setters
    public Long getEventId() {
        return eventId;
    }

    public void setEventId(Long eventId) {
        this.eventId = eventId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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
}
