package com.group02.openevent.model.event;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("OTHERS")
public class OtherEvent extends Event {

    @Column(columnDefinition = "TEXT")
    private String customNote;

    public OtherEvent() {
    }

    public OtherEvent(String otherInfo) {
        this.customNote = otherInfo;
    }

    public String getCustomNote() {
        return customNote;
    }

    public void setCustomNote(String otherInfo) {
        this.customNote = otherInfo;
    }
}
