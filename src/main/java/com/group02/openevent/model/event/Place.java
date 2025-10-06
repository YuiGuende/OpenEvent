package com.group02.openevent.model.event;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.group02.openevent.model.enums.Building;
import jakarta.persistence.*;

import java.util.List;

@Entity
@Table(name = "place")
public class Place {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "place_id")
    private Integer id;

    @ManyToMany(mappedBy = "places")
    @JsonIgnore
    private List<Event> events;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Building building = Building.NONE;

    @Column(name = "place_name", nullable = false, length = 150)
    private String placeName;

    public Place() {
    }

    public Place(Integer id, List<Event> events, Building building, String placeName) {
        this.id = id;
        this.events = events;
        this.building = building;
        this.placeName = placeName;
    }

    public Place(Building building, String placeName) {
        this.building = building;
        this.placeName = placeName;
    }

    // Getter & Setter
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Building getBuilding() {
        return building;
    }

    public void setBuilding(Building building) {
        this.building = building;
    }

    public String getPlaceName() {
        return placeName;
    }

    public void setPlaceName(String placeName) {
        this.placeName = placeName;
    }

    public List<Event> getEvents() {
        return events;
    }

    public void setEvents(List<Event> events) {
        this.events = events;
    }

    @Override
    public String toString() {
        return "Place{" +
                "id=" + id +
                ", building=" + building +
                ", placeName='" + placeName + '\'' +
                '}';
    }
}
