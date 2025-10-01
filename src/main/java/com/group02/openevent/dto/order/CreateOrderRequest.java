package com.group02.openevent.dto.order;

public class CreateOrderRequest {
    private Long eventId;
    private String participantName;
    private String participantEmail;
    private String participantPhone;
    private String participantOrganization;
    private String notes;

    public Long getEventId() { return eventId; }
    public void setEventId(Long eventId) { this.eventId = eventId; }

    public String getParticipantName() { return participantName; }
    public void setParticipantName(String participantName) { this.participantName = participantName; }

    public String getParticipantEmail() { return participantEmail; }
    public void setParticipantEmail(String participantEmail) { this.participantEmail = participantEmail; }

    public String getParticipantPhone() { return participantPhone; }
    public void setParticipantPhone(String participantPhone) { this.participantPhone = participantPhone; }

    public String getParticipantOrganization() { return participantOrganization; }
    public void setParticipantOrganization(String participantOrganization) { this.participantOrganization = participantOrganization; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}


