package com.group02.openevent.model.dto.event;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.group02.openevent.model.dto.PlaceDTO;
import com.group02.openevent.model.dto.ScheduleDTO;
import com.group02.openevent.model.dto.SpeakerDTO;
import com.group02.openevent.model.enums.EventType;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.util.List;

public class WorkshopEventDetailDTO {
    // Các trường chung kế thừa từ Event
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
    private LocalDateTime updatedAt;
    private EventType eventType;
    private String benefits;

    // Các trường mở rộng
    private List<String> imageUrls;
    private List<SpeakerDTO> speakers;
    private List<ScheduleDTO> schedules;
    private List<PlaceDTO> places;
    private String venueAddress;
    private String guidelines;

    // Các trường đặc thù của WorkshopEvent
    private String topic;
    private String materialsLink;
    private Integer maxParticipants;
    private String skillLevel;
    private String prerequisites;


    public WorkshopEventDetailDTO() {
    }

    public WorkshopEventDetailDTO(String description, String title, Integer capacity, LocalDateTime startsAt, LocalDateTime endsAt, LocalDateTime createdAt, LocalDateTime updatedAt, EventType eventType, String benefits, List<String> imageUrls, List<SpeakerDTO> speakers, List<ScheduleDTO> schedules, List<PlaceDTO> places, String venueAddress, String guidelines, String topic, String materialsLink, Integer maxParticipants, String skillLevel, String prerequisites) {
        this.description = description;
        this.title = title;
        this.capacity = capacity;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.eventType = eventType;
        this.benefits = benefits;
        this.imageUrls = imageUrls;
        this.speakers = speakers;
        this.schedules = schedules;
        this.places = places;
        this.venueAddress = venueAddress;
        this.guidelines = guidelines;
        this.topic = topic;
        this.materialsLink = materialsLink;
        this.maxParticipants = maxParticipants;
        this.skillLevel = skillLevel;
        this.prerequisites = prerequisites;
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

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
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

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getMaterialsLink() {
        return materialsLink;
    }

    public void setMaterialsLink(String materialsLink) {
        this.materialsLink = materialsLink;
    }

    public Integer getMaxParticipants() {
        return maxParticipants;
    }

    public void setMaxParticipants(Integer maxParticipants) {
        this.maxParticipants = maxParticipants;
    }

    public String getSkillLevel() {
        return skillLevel;
    }

    public void setSkillLevel(String skillLevel) {
        this.skillLevel = skillLevel;
    }

    public String getPrerequisites() {
        return prerequisites;
    }

    public void setPrerequisites(String prerequisites) {
        this.prerequisites = prerequisites;
    }
}