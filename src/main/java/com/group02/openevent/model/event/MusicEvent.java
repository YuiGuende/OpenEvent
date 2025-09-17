package com.group02.openevent.model.event;

import jakarta.persistence.*;

@Entity
@DiscriminatorValue("MUSIC")
public class MusicEvent extends Event {
    @Column(nullable = false)
    private String artist;

    private String genre;

    @Column(name = "stage_name")
    private String stageName;

    public MusicEvent() {
    }


    public MusicEvent(String artist, String genre, String stageName) {
        this.artist = artist;
        this.genre = genre;
        this.stageName = stageName;
    }
    // Getter & Setter

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public String getStageName() {
        return stageName;
    }

    public void setStageName(String stageName) {
        this.stageName = stageName;
    }

    @Override
    public String toString() {
        return "MusicEvent{" +
                ", artist='" + artist + '\'' +
                ", genre='" + genre + '\'' +
                ", stageName='" + stageName + '\'' +
                '}';
    }
}
