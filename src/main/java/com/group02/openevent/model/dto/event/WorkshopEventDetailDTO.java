package com.group02.openevent.model.dto.event;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.group02.openevent.model.dto.*;
import com.group02.openevent.model.enums.EventStatus;
import com.group02.openevent.model.enums.EventType;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Getter
@Setter
public class WorkshopEventDetailDTO {
    // Thông tin cơ bản kế thừa từ Event
    private String title;
    private String description;
    private Integer capacity;
    private String benefits;
    private String guidelines;
    private String venueAddress;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime startsAt;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime endsAt;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime enrollDeadline;

    private String bannerUrl;
    private List<String> galleryUrls;

    // ⭐ CÁC TRƯỜNG ĐẶC THÙ CỦA WORKSHOP EVENT
    private String topic;
    private String materialsLink;
    private Integer maxParticipants;
    private String skillLevel;
    private String prerequisites;

    // Các danh sách liên quan
    private List<SpeakerDTO> speakers;
    private List<ScheduleDTO> schedules;
    private Map<LocalDate, List<ScheduleDTO>> schedulesByDay;
    private List<PlaceDTO> places;
    private List<TicketTypeDTO> ticketTypes;

    // Thông tin nhà tổ chức và trạng thái
    private OrganizationDTO organization;
    private EventStatus status;

    // Metadata
    private EventType eventType;
    private LocalDateTime createdAt;

    // Constructor rỗng
    public WorkshopEventDetailDTO() {}

    public WorkshopEventDetailDTO(String title, String description, Integer capacity, String benefits, String guidelines, String venueAddress, LocalDateTime startsAt, LocalDateTime endsAt, LocalDateTime enrollDeadline, String bannerUrl, List<String> galleryUrls, String topic, String materialsLink, Integer maxParticipants, String skillLevel, String prerequisites, List<SpeakerDTO> speakers, List<ScheduleDTO> schedules, Map<LocalDate, List<ScheduleDTO>> schedulesByDay, List<PlaceDTO> places, List<TicketTypeDTO> ticketTypes, OrganizationDTO organization, EventStatus status, EventType eventType, LocalDateTime createdAt) {
        this.title = title;
        this.description = description;
        this.capacity = capacity;
        this.benefits = benefits;
        this.guidelines = guidelines;
        this.venueAddress = venueAddress;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
        this.enrollDeadline = enrollDeadline;
        this.bannerUrl = bannerUrl;
        this.galleryUrls = galleryUrls;
        this.topic = topic;
        this.materialsLink = materialsLink;
        this.maxParticipants = maxParticipants;
        this.skillLevel = skillLevel;
        this.prerequisites = prerequisites;
        this.speakers = speakers;
        this.schedules = schedules;
        this.schedulesByDay = schedulesByDay;
        this.places = places;
        this.ticketTypes = ticketTypes;
        this.organization = organization;
        this.status = status;
        this.eventType = eventType;
        this.createdAt = createdAt;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
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

    public String getBenefits() {
        return benefits;
    }

    public void setBenefits(String benefits) {
        this.benefits = benefits;
    }

    public String getGuidelines() {
        return guidelines;
    }

    public void setGuidelines(String guidelines) {
        this.guidelines = guidelines;
    }

    public String getVenueAddress() {
        return venueAddress;
    }

    public void setVenueAddress(String venueAddress) {
        this.venueAddress = venueAddress;
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

    public LocalDateTime getEnrollDeadline() {
        return enrollDeadline;
    }

    public void setEnrollDeadline(LocalDateTime enrollDeadline) {
        this.enrollDeadline = enrollDeadline;
    }

    public String getBannerUrl() {
        return bannerUrl;
    }

    public void setBannerUrl(String bannerUrl) {
        this.bannerUrl = bannerUrl;
    }

    public List<String> getGalleryUrls() {
        return galleryUrls;
    }

    public void setGalleryUrls(List<String> galleryUrls) {
        this.galleryUrls = galleryUrls;
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

    public Map<LocalDate, List<ScheduleDTO>> getSchedulesByDay() {
        return schedulesByDay;
    }

    public void setSchedulesByDay(Map<LocalDate, List<ScheduleDTO>> schedulesByDay) {
        this.schedulesByDay = schedulesByDay;
    }

    public List<PlaceDTO> getPlaces() {
        return places;
    }

    public void setPlaces(List<PlaceDTO> places) {
        this.places = places;
    }

    public List<TicketTypeDTO> getTicketTypes() {
        return ticketTypes;
    }

    public void setTicketTypes(List<TicketTypeDTO> ticketTypes) {
        this.ticketTypes = ticketTypes;
    }

    public OrganizationDTO getOrganization() {
        return organization;
    }

    public void setOrganization(OrganizationDTO organization) {
        this.organization = organization;
    }

    public EventStatus getStatus() {
        return status;
    }

    public void setStatus(EventStatus status) {
        this.status = status;
    }

    public EventType getEventType() {
        return eventType;
    }

    public void setEventType(EventType eventType) {
        this.eventType = eventType;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}