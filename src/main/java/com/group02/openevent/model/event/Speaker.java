package com.group02.openevent.model.event;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.group02.openevent.model.enums.SpeakerRole;
import jakarta.persistence.*;

import java.util.List;

@Entity
@Table(name="speaker")

public class Speaker {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "speaker_id")
    private Integer id;

    @Column(nullable = true, length = 100)
    private String name;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "profile")
    private String profile;

    @Enumerated(EnumType.STRING)
    @Column(name = "default_role", nullable = false)
    private SpeakerRole defaultRole = SpeakerRole.SPEAKER;

    @ManyToMany(mappedBy = "speakers")
    @JsonIgnore
    private List<Event> events;

    public Speaker() {
    }

    // Getters & Setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getProfile() {
        return profile;
    }

    public void setProfile(String profile) {
        this.profile = profile;
    }

    public SpeakerRole getDefaultRole() {
        return defaultRole;
    }

    public void setDefaultRole(SpeakerRole defaultRole) {
        this.defaultRole = defaultRole;
    }

    public List<Event> getEvents() {
        return events;
    }

    public void setEvents(List<Event> events) {
        this.events = events;
    }
}
