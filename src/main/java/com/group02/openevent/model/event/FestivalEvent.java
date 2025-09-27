package com.group02.openevent.model.event;

import jakarta.persistence.*;

@Entity
@DiscriminatorValue("FestivalEvent")
public class FestivalEvent extends Event {

    private String culture;

    @Column(columnDefinition = "TEXT")
    private String highlight;

    public FestivalEvent() {
    }

    public FestivalEvent(String culture, String highlight) {

        this.culture = culture;
        this.highlight = highlight;
    }
    // Getter & Setter


    public String getCulture() {
        return culture;
    }

    public void setCulture(String culture) {
        this.culture = culture;
    }

    public String getHighlight() {
        return highlight;
    }

    public void setHighlight(String highlight) {
        this.highlight = highlight;
    }

    @Override
    public String toString() {
        return "FestivalEvent{" +
                ", culture='" + culture + '\'' +
                ", highlight='" + highlight + '\'' +
                '}';
    }
}
