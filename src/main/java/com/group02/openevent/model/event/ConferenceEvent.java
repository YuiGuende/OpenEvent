package com.group02.openevent.model.event;

import jakarta.persistence.*;

@Entity
@DiscriminatorValue("CONFERENCE")
public class ConferenceEvent extends Event {

    @Column(name = "conference_type")
    private String conferenceType;

    @Column(name = "max_attendees")
    private Integer maxAttendees;

    @Column(name = "agenda")
    private String agenda;

    public ConferenceEvent() {
    }

    public ConferenceEvent(String conferenceType, Integer maxAttendees, String agenda) {
        this.conferenceType = conferenceType;
        this.maxAttendees = maxAttendees;
        this.agenda = agenda;
    }

    // Getters and Setters
    public String getConferenceType() {
        return conferenceType;
    }

    public void setConferenceType(String conferenceType) {
        this.conferenceType = conferenceType;
    }

    public Integer getMaxAttendees() {
        return maxAttendees;
    }

    public void setMaxAttendees(Integer maxAttendees) {
        this.maxAttendees = maxAttendees;
    }

    public String getAgenda() {
        return agenda;
    }

    public void setAgenda(String agenda) {
        this.agenda = agenda;
    }

    @Override
    public String toString() {
        return "ConferenceEvent{" +
                "conferenceType='" + conferenceType + '\'' +
                ", maxAttendees=" + maxAttendees +
                ", agenda='" + agenda + '\'' +
                '}';
    }
}
