package com.group02.openevent.model.event;

import jakarta.persistence.*;

@Entity
@Table(
        name = "place",
        uniqueConstraints = @UniqueConstraint(columnNames = {"event_id", "place_name"})
)
public class Place {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "place_id")
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Building building = Building.NONE;

    @Column(name = "place_name", nullable = false, length = 150)
    private String placeName;

    public Place() {
    }

    public Place(Event event, Building building, String placeName) {
        this.event = event;
        this.building = building;
        this.placeName = placeName;
    }

    public Place(Integer id, String placeName, Building building, Event event) {
        this.id = id;
        this.placeName = placeName;
        this.building = building;
        this.event = event;
    }

    // Getter & Setter
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Event getEvent() {
        return event;
    }

    public void setEvent(Event event) {
        this.event = event;
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

    @Override
    public String toString() {
        return "Place{" +
                "id=" + id +
                ", event=" + event +
                ", building=" + building +
                ", placeName='" + placeName + '\'' +
                '}';
    }
}
