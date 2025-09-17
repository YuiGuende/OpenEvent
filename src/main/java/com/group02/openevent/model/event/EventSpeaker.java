package com.group02.openevent.model.event;

import jakarta.persistence.*;

@Entity
public class EventSpeaker {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "event_speaker_id")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "speaker_id", nullable = false)
    private Speaker speaker;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SpeakerRole role;  // Vai trò cụ thể của speaker trong event này

    private String note; // thêm thông tin phụ, ví dụ "Closing keynote" hay "Guest performer"

    public EventSpeaker() {
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Event getEvent() {
        return event;
    }

    public void setEvent(Event event) {
        this.event = event;
    }

    public Speaker getSpeaker() {
        return speaker;
    }

    public void setSpeaker(Speaker speaker) {
        this.speaker = speaker;
    }

    public SpeakerRole getRole() {
        return role;
    }

    public void setRole(SpeakerRole role) {
        this.role = role;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
