package com.group02.openevent.model.event;

import jakarta.persistence.*;

@Entity
@DiscriminatorValue("MUSIC")
public class MusicEvent extends Event {
    @Column(name = "music_type")
    String musicType;

    public String getMusicType() {
        return musicType;
    }

    public void setMusicType(String musicType) {
        this.musicType = musicType;
    }

    public MusicEvent() {
    }

}
