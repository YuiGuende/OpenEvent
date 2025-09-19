package com.group02.openevent.model.event;

import jakarta.persistence.*;

@Entity
@DiscriminatorValue("MUSIC")
public class MusicEvent extends Event {

    public MusicEvent() {
    }

}
