package com.group02.openevent.model.event;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.group02.openevent.model.enums.EventStatus;
import com.group02.openevent.model.enums.EventType;
import com.group02.openevent.model.organization.Organization;
import com.group02.openevent.model.ticket.TicketType;
import com.group02.openevent.model.user.Host;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "event")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "event_type", discriminatorType = DiscriminatorType.STRING)
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXISTING_PROPERTY,
        property = "eventType",
        visible = true
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = MusicEvent.class, name = "MUSIC"),
        @JsonSubTypes.Type(value = WorkshopEvent.class, name = "WORKSHOP"),
        @JsonSubTypes.Type(value = FestivalEvent.class, name = "FESTIVAL"),
        @JsonSubTypes.Type(value = CompetitionEvent.class, name = "COMPETITION"),
        @JsonSubTypes.Type(value = OtherEvent.class, name = "OTHERS")
})

public class Event {

    @Id
    @SequenceGenerator(
            name = "event_sequence",
            sequenceName = "event_sequence",
            allocationSize = 1
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "event_sequence"
    )
    private Long id;

    private boolean poster;

    @ManyToOne
    @JoinColumn(name = "parent_event_id")
    @JsonBackReference
    private Event parentEvent;

    @OneToMany(mappedBy = "parentEvent", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<Event> subEvents;

    @Column(name = "event_title", nullable = false, length = 150)
    private String title;

    @Column(name = "image_url",columnDefinition = "LONGTEXT")
    private String imageUrl;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "capacity")
    private Integer capacity;

    @Column(name = "public_date")
    private LocalDateTime publicDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, insertable = false, updatable = false)
    private EventType eventType = EventType.OTHERS;

    @Column(name = "enroll_deadline", nullable = false)
    private LocalDateTime enrollDeadline;

    @Column(name = "starts_at", nullable = false)
    private LocalDateTime startsAt;

    @Column(name = "ends_at", nullable = false)
    private LocalDateTime endsAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private EventStatus status = EventStatus.DRAFT;

    @Column(columnDefinition = "TEXT")
    private String benefits;

    @Column(name = "learning_objects", columnDefinition = "TEXT")
    private String learningObjects;

    private Integer points;

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<EventSchedule> schedules = new ArrayList<>();

    @ManyToMany(cascade = CascadeType.PERSIST)
    @JoinTable(name = "event_speaker",
            joinColumns = @JoinColumn(name = "event_id"),
            inverseJoinColumns = @JoinColumn(name = "speaker_id"))
    private List<Speaker> speakers = new ArrayList<>();

    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(name = "event_place",
            joinColumns = @JoinColumn(name = "event_id"),
            inverseJoinColumns = @JoinColumn(name = "place_id"))
    private List<Place> places;

    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "event_id")
    private Set<EventImage> eventImages;

    @ManyToOne
    @JoinColumn(name = "org_id", nullable = true,
            foreignKey = @ForeignKey(name = "fk_event_org"))
    private Organization organization;

    @ManyToOne
    @JoinColumn(name = "host_id", nullable = true,
            foreignKey = @ForeignKey(name = "fk_event_host"))
    private Host host;

    @OneToMany( cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "event_id")
    private List<TicketType> ticketTypes = new ArrayList<>();


    public Event() {
    }

    public Event(Long id, Event parentEvent, List<Event> subEvents, String title, String imageUrl, String description, Integer capacity, LocalDateTime publicDate, EventType eventType, LocalDateTime enrollDeadline, LocalDateTime startsAt, LocalDateTime endsAt, LocalDateTime createdAt, EventStatus status, String benefits, String learningObjects, Integer points, List<EventSchedule> schedules, List<Speaker> speakers, List<Place> places, Set<EventImage> eventImages) {
        this.id = id;
        this.parentEvent = parentEvent;
        this.subEvents = subEvents;
        this.title = title;
        this.imageUrl = imageUrl;
        this.description = description;
        this.capacity = capacity;
        this.publicDate = publicDate;
        this.eventType = eventType;
        this.enrollDeadline = enrollDeadline;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
        this.createdAt = createdAt;
        this.status = status;
        this.benefits = benefits;
        this.learningObjects = learningObjects;
        this.points = points;
        this.schedules = schedules;
        this.speakers = speakers;
        this.places = places;
        this.eventImages = eventImages;
    }

    // Getter & Setter


    public Host getHost() {
        return host;
    }

    public void setHost(Host host) {
        this.host = host;
    }

    public double getMaxTicketPice() {
        double maxTicketPice = 0;
        for (TicketType ticketType : ticketTypes) {
            if (ticketType.getPrice().doubleValue() > maxTicketPice) {
                maxTicketPice = ticketType.getPrice().doubleValue();
            }
        }
        return maxTicketPice;

    }
    public double getMinTicketPice() {
        double minTicketPice = 0;
        for (TicketType ticketType : ticketTypes) {
            if (ticketType.getPrice().doubleValue() < minTicketPice) {
                minTicketPice = ticketType.getPrice().doubleValue();
            }
        }
        return minTicketPice;

    }

    public Integer getCapacity() {
        return capacity;
    }

    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }

    public List<Speaker> getSpeakers() {
        return speakers;
    }

    public void setSpeakers(List<Speaker> speakers) {
        this.speakers = speakers;
    }

    public LocalDateTime getPublicDate() {
        return publicDate;
    }

    public void setPublicDate(LocalDateTime publicDate) {
        this.publicDate = publicDate;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Event getParentEvent() {
        return parentEvent;
    }

    public void setParentEvent(Event parentEvent) {
        this.parentEvent = parentEvent;
    }

    public List<Event> getSubEvents() {
        return subEvents;
    }

    public void setSubEvents(List<Event> subEvents) {
        this.subEvents = subEvents;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public EventType getEventType() {
        return eventType;
    }

    public void setEventType(EventType eventType) {
        this.eventType = eventType;
    }

    public LocalDateTime getEnrollDeadline() {
        return enrollDeadline;
    }

    public void setEnrollDeadline(LocalDateTime enrollDeadline) {
        this.enrollDeadline = enrollDeadline;
    }

    public LocalDateTime getStartsAt() {
        return startsAt;
    }

    public void setStartsAt(LocalDateTime startsAt) {
        this.startsAt = startsAt;
    }

    public LocalDateTime getEndsAt() {
        return endsAt;
    }

    public void setEndsAt(LocalDateTime endsAt) {
        this.endsAt = endsAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public EventStatus getStatus() {
        return status;
    }

    public void setStatus(EventStatus status) {
        this.status = status;
    }

    public String getBenefits() {
        return benefits;
    }

    public void setBenefits(String benefits) {
        this.benefits = benefits;
    }

    public String getLearningObjects() {
        return learningObjects;
    }

    public void setLearningObjects(String learningObjects) {
        this.learningObjects = learningObjects;
    }


    public Integer getPoints() {
        return points;
    }

    public void setPoints(Integer points) {
        this.points = points;
    }

    public List<Place> getPlaces() {
        return places;
    }

    public void setPlaces(List<Place> places) {
        this.places = places;
    }

    public List<EventSchedule> getSchedules() {
        return schedules;
    }

    public void setSchedules(List<EventSchedule> schedules) {
        this.schedules = schedules;
    }

    public Set<EventImage> getEventImages() {
        return eventImages;
    }

    public void setEventImages(Set<EventImage> eventImages) {
        this.eventImages = eventImages;
    }

    @Override
    public String toString() {
        return "Event{" +
                "id=" + id +
                ", parentEvent=" + parentEvent +
                ", subEvents=" + subEvents +
                ", title='" + title + '\'' +
                ", imageUrl='" + imageUrl + '\'' +
                ", description='" + description + '\'' +
                ", publicDate=" + publicDate +
                ", eventType=" + eventType +
                ", enrollDeadline=" + enrollDeadline +
                ", startsAt=" + startsAt +
                ", endsAt=" + endsAt +
                ", createdAt=" + createdAt +
                ", status=" + status +
                ", benefits='" + benefits + '\'' +
                ", learningObjects='" + learningObjects + '\'' +
                ", schedules='" + schedules + '\'' +
                ", points=" + points +
                ", places=" + places +
                '}';
    }

    public boolean isPoster() {
        return poster;
    }

    public void setPoster(boolean poster) {
        this.poster = poster;
    }

    public Organization getOrganization() {
        return organization;
    }

    public void setOrganization(Organization organization) {
        this.organization = organization;
    }
}
