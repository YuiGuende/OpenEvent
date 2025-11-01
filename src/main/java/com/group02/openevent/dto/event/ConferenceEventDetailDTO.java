package com.group02.openevent.dto.event;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.group02.openevent.dto.PlaceDTO;
import com.group02.openevent.dto.ScheduleDTO;
import com.group02.openevent.dto.SpeakerDTO;
import com.group02.openevent.model.enums.EventType;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.util.List;

public class ConferenceEventDetailDTO {
    // Các trường chung từ Event
    private String description;
    private String title;
    private Integer capacity;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime startsAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime endsAt;

    private LocalDateTime createdAt;
    private EventType eventType;
    private String benefits;

    // Các trường mở rộng
    private List<String> imageUrls;
    private List<SpeakerDTO> speakers;
    private List<ScheduleDTO> schedules;
    private List<PlaceDTO> places;
    private String venueAddress;
    private String guidelines;

    // Các trường đặc thù của ConferenceEvent
    private String conferenceType;
    private Integer maxAttendees;
    private String agenda;

    public ConferenceEventDetailDTO() {
    }

    // Constructor đầy đủ
    public ConferenceEventDetailDTO(String description, String title, Integer capacity, LocalDateTime startsAt, LocalDateTime endsAt, LocalDateTime createdAt, EventType eventType, String benefits, List<String> imageUrls, List<SpeakerDTO> speakers, List<ScheduleDTO> schedules, List<PlaceDTO> places, String venueAddress, String guidelines, String conferenceType, Integer maxAttendees, String agenda) {
        this.description = description;
        this.title = title;
        this.capacity = capacity;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
        this.createdAt = createdAt;
        this.eventType = eventType;
        this.benefits = benefits;
        this.imageUrls = imageUrls;
        this.speakers = speakers;
        this.schedules = schedules;
        this.places = places;
        this.venueAddress = venueAddress;
        this.guidelines = guidelines;
        this.conferenceType = conferenceType;
        this.maxAttendees = maxAttendees;
        this.agenda = agenda;
    }


    // Getters and Setters cho tất cả các trường

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
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

    public EventType getEventType() {
        return eventType;
    }

    public void setEventType(EventType eventType) {
        this.eventType = eventType;
    }

    public String getBenefits() {
        return benefits;
    }

    public void setBenefits(String benefits) {
        this.benefits = benefits;
    }

    public List<String> getImageUrls() {
        return imageUrls;
    }

    public void setImageUrls(List<String> imageUrls) {
        this.imageUrls = imageUrls;
    }

    public List<SpeakerDTO> getSpeakers() {
        return speakers;
    }

    public void setSpeakers(List<SpeakerDTO> speakers) {
        this.speakers = speakers;
    }

    public List<ScheduleDTO> getSchedules() {
        return schedules;
    }

    public void setSchedules(List<ScheduleDTO> schedules) {
        this.schedules = schedules;
    }

    public List<PlaceDTO> getPlaces() {
        return places;
    }

    public void setPlaces(List<PlaceDTO> places) {
        this.places = places;
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

    public String getConferenceType() {
        return conferenceType;
    }

    public void setConferenceType(String conferenceType) {
        this.conferenceType = conferenceType;
    }

    public Integer getMaxAttendees() {
        return maxAttendees;
    }

    public void setMaxAttendees(Integer maxAttendees) {
        this.maxAttendees = maxAttendees;
    }

    public String getAgenda() {
        return agenda;
    }

    public void setAgenda(String agenda) {
        this.agenda = agenda;
    }
}