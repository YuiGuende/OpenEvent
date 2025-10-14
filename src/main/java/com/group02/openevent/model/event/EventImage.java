package com.group02.openevent.model.event;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;

import java.util.Objects;

@Entity
@Table
public class EventImage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String url;
    
    private String alt;

    private int orderIndex;

    private boolean mainPoster = false;
    @ManyToOne
    @JsonBackReference
    private Event event;

    public EventImage() {
    }

    public EventImage(String url, int orderIndex, boolean mainPoster, Event event) {
        this.url = url;
        this.orderIndex = orderIndex;
        this.mainPoster = mainPoster;
        this.event = event;
    }

    public EventImage(String url, int orderIndex, boolean mainPoster) {
        this.url = url;
        this.orderIndex = orderIndex;
        this.mainPoster = mainPoster;
    }


    public Event getEvent() {
        return event;
    }

    public void setEvent(Event event) {
        this.event = event;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getAlt() {
        return alt;
    }

    public void setAlt(String alt) {
        this.alt = alt;
    }

    public int getOrderIndex() {
        return orderIndex;
    }

    public void setOrderIndex(int index) {
        this.orderIndex = index;
    }

    public boolean isMainPoster() {
        return mainPoster;
    }

    public void setMainPoster(boolean mainPoster) {
        this.mainPoster = mainPoster;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        EventImage that = (EventImage) o;
        return Objects.equals(url, that.url);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(url);
    }

    @Override
    public String toString() {
        return "EventImage{" +
                "id=" + id +
                ", url='" + url + '\'' +
                ", index=" + orderIndex +
                ", mainPoster=" + mainPoster +
                '}';
    }
}
