package com.group02.openevent.model.event;

import jakarta.persistence.*;

import java.util.List;

@Entity
@DiscriminatorValue("MusicEvent")
public class MusicEvent extends Event {

    @Column(name = "genre")
    private String genre;

    @Column(name = "performer_count")
    private Integer performerCount;

    public MusicEvent() {

    }

    public MusicEvent(String genre, Integer performerCount) {
        this.genre = genre;
        this.performerCount = performerCount;
    }

    // Getters and Setters
    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public Integer getPerformerCount() {
        return performerCount;
    }

    public void setPerformerCount(Integer performerCount) {
        this.performerCount = performerCount;
    }

}
