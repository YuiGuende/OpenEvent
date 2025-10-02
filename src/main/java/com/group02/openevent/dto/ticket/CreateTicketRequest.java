package com.group02.openevent.dto.ticket;

import java.math.BigDecimal;

public class CreateTicketRequest {
    private Long eventId;
    private BigDecimal price;
    private String description;
    private String participantName;
    private String participantEmail;
    private String participantPhone;
    private String participantOrganization;
    private String notes;
    private String ticketTypeName;

    public CreateTicketRequest() {}

    public CreateTicketRequest(Long eventId, BigDecimal price, String description, 
                             String participantName, String participantEmail, String participantPhone, 
                             String participantOrganization, String notes, String ticketTypeName) {
        this.eventId = eventId;
        this.price = price;
        this.description = description;
        this.participantName = participantName;
        this.participantEmail = participantEmail;
        this.participantPhone = participantPhone;
        this.participantOrganization = participantOrganization;
        this.notes = notes;
        this.ticketTypeName = ticketTypeName;
    }

    // Getters and Setters
    public Long getEventId() {
        return eventId;
    }

    public void setEventId(Long eventId) {
        this.eventId = eventId;
    }


    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
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

    public String getTicketTypeName() {
        return ticketTypeName;
    }

    public void setTicketTypeName(String ticketTypeName) {
        this.ticketTypeName = ticketTypeName;
    }
}
