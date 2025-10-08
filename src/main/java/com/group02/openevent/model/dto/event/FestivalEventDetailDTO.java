package com.group02.openevent.model.dto.event;

import com.group02.openevent.model.dto.ScheduleDTO;
import com.group02.openevent.model.enums.EventStatus;
import com.group02.openevent.model.event.EventImage;
import com.group02.openevent.model.event.Place;
import com.group02.openevent.model.event.Speaker;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

public class FestivalEventDetailDTO {
    private Long id;
    private String title;
    private String imageUrl;
    private String description;
    private Integer capacity;
    private LocalDateTime publicDate;
    private LocalDateTime enrollDeadline;
    private LocalDateTime startsAt;
    private LocalDateTime endsAt;
    private LocalDateTime createdAt;
    private EventStatus status;
    private String benefits;
    private String learningObjects;
    private Integer points;
    private String venueAddress;
    private String guidelines;

    private String culture;
    private String highlight;

    private String festivalTheme;
    private Integer numberOfActivities;
    private String targetAudience;
    private Boolean registrationRequired;
    private String festivalType;
    private String mainOrganizer;
    private Integer expectedAttendees;

    private List<ScheduleDTO> schedules;
    private List<Speaker> speakers;
    private List<Place> places;
    private Set<EventImage> eventImages;
    private String organizationName;

    public FestivalEventDetailDTO() {
    }

    // Constructor
    public FestivalEventDetailDTO(Long id, String title, String imageUrl, String description,
                                  Integer capacity, LocalDateTime publicDate, LocalDateTime enrollDeadline,
                                  LocalDateTime startsAt, LocalDateTime endsAt, LocalDateTime createdAt,
                                  EventStatus status, String benefits, String learningObjects, Integer points,
                                  String venueAddress, String guidelines, String culture, String highlight,
                                  String festivalTheme, Integer numberOfActivities, String targetAudience,
                                  Boolean registrationRequired, String festivalType, String mainOrganizer,
                                  Integer expectedAttendees, List<ScheduleDTO> schedules, List<Speaker> speakers,
                                  List<Place> places, Set<EventImage> eventImages, String organizationName) {
        this.id = id;
        this.title = title;
        this.imageUrl = imageUrl;
        this.description = description;
        this.capacity = capacity;
        this.publicDate = publicDate;
        this.enrollDeadline = enrollDeadline;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
        this.createdAt = createdAt;
        this.status = status;
        this.benefits = benefits;
        this.learningObjects = learningObjects;
        this.points = points;
        this.venueAddress = venueAddress;
        this.guidelines = guidelines;
        this.culture = culture;
        this.highlight = highlight;
        this.festivalTheme = festivalTheme;
        this.numberOfActivities = numberOfActivities;
        this.targetAudience = targetAudience;
        this.registrationRequired = registrationRequired;
        this.festivalType = festivalType;
        this.mainOrganizer = mainOrganizer;
        this.expectedAttendees = expectedAttendees;
        this.schedules = schedules;
        this.speakers = speakers;
        this.places = places;
        this.eventImages = eventImages;
        this.organizationName = organizationName;
    }

    // Getters & Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public Integer getCapacity() {
        return capacity;
    }

    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }

    public LocalDateTime getPublicDate() {
        return publicDate;
    }

    public void setPublicDate(LocalDateTime publicDate) {
        this.publicDate = publicDate;
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

    public String getVenueAddress() {
        return venueAddress;
    }

    public void setVenueAddress(String venueAddress) {
        this.venueAddress = venueAddress;
    }

    public String getGuidelines() {
        return guidelines;
    }

    public void setGuidelines(String guidelines) {
        this.guidelines = guidelines;
    }

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

    public String getFestivalTheme() {
        return festivalTheme;
    }

    public void setFestivalTheme(String festivalTheme) {
        this.festivalTheme = festivalTheme;
    }

    public Integer getNumberOfActivities() {
        return numberOfActivities;
    }

    public void setNumberOfActivities(Integer numberOfActivities) {
        this.numberOfActivities = numberOfActivities;
    }

    public String getTargetAudience() {
        return targetAudience;
    }

    public void setTargetAudience(String targetAudience) {
        this.targetAudience = targetAudience;
    }

    public Boolean getRegistrationRequired() {
        return registrationRequired;
    }

    public void setRegistrationRequired(Boolean registrationRequired) {
        this.registrationRequired = registrationRequired;
    }

    public String getFestivalType() {
        return festivalType;
    }

    public void setFestivalType(String festivalType) {
        this.festivalType = festivalType;
    }

    public String getMainOrganizer() {
        return mainOrganizer;
    }

    public void setMainOrganizer(String mainOrganizer) {
        this.mainOrganizer = mainOrganizer;
    }

    public Integer getExpectedAttendees() {
        return expectedAttendees;
    }

    public void setExpectedAttendees(Integer expectedAttendees) {
        this.expectedAttendees = expectedAttendees;
    }

    public List<ScheduleDTO> getSchedules() {
        return schedules;
    }

    public void setSchedules(List<ScheduleDTO> schedules) {
        this.schedules = schedules;
    }

    public List<Speaker> getSpeakers() {
        return speakers;
    }

    public void setSpeakers(List<Speaker> speakers) {
        this.speakers = speakers;
    }

    public List<Place> getPlaces() {
        return places;
    }

    public void setPlaces(List<Place> places) {
        this.places = places;
    }

    public Set<EventImage> getEventImages() {
        return eventImages;
    }

    public void setEventImages(Set<EventImage> eventImages) {
        this.eventImages = eventImages;
    }

    public String getOrganizationName() {
        return organizationName;
    }

    public void setOrganizationName(String organizationName) {
        this.organizationName = organizationName;
    }


}
