package com.group02.openevent.model.event;

import jakarta.persistence.*;

import java.util.List;

@Entity
@DiscriminatorValue("MUSIC")
public class MusicEvent extends Event {

    public MusicEvent() {
    }

}
