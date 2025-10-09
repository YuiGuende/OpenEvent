package com.group02.openevent.model.dto.event;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.group02.openevent.model.dto.*;
import com.group02.openevent.model.enums.EventStatus;
import com.group02.openevent.model.enums.EventType;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class MusicEventDetailDTO {
    // Thông tin cơ bản
    private String title;
    private String description;
    private Integer capacity;
    private String benefits;
    private String guidelines;
    private String venueAddress;

    // Thông tin về thời gian
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime startsAt;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime endsAt;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime enrollDeadline;

    // Thông tin hình ảnh
    private String bannerUrl;
    private List<String> galleryUrls;

    // Thông tin chuyên biệt của sự kiện âm nhạc
    private String musicType;
    private String genre;

    // Các danh sách liên quan
    private List<SpeakerDTO> speakers;
    private List<ScheduleDTO> schedules;
    private Map<LocalDate, List<ScheduleDTO>> schedulesByDay; // For 2-column schedule display
    private List<PlaceDTO> places;
    private List<TicketTypeDTO> ticketTypes; // <--- TRƯỜNG BẠN ĐANG THIẾU

    // Thông tin nhà tổ chức và trạng thái
    private OrganizationDTO organization;
    private EventStatus status;

    // Metadata
    private EventType eventType;
    private LocalDateTime createdAt;

    // Constructor rỗng
    public MusicEventDetailDTO() {
    }

    // GETTERS AND SETTERS CHO TẤT CẢ CÁC TRƯỜNG

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Integer getCapacity() { return capacity; }
    public void setCapacity(Integer capacity) { this.capacity = capacity; }

    public String getBenefits() { return benefits; }
    public void setBenefits(String benefits) { this.benefits = benefits; }

    public String getGuidelines() { return guidelines; }
    public void setGuidelines(String guidelines) { this.guidelines = guidelines; }

    public String getVenueAddress() { return venueAddress; }
    public void setVenueAddress(String venueAddress) { this.venueAddress = venueAddress; }

    public LocalDateTime getStartsAt() { return startsAt; }
    public void setStartsAt(LocalDateTime startsAt) { this.startsAt = startsAt; }

    public LocalDateTime getEndsAt() { return endsAt; }
    public void setEndsAt(LocalDateTime endsAt) { this.endsAt = endsAt; }

    public LocalDateTime getEnrollDeadline() { return enrollDeadline; }
    public void setEnrollDeadline(LocalDateTime enrollDeadline) { this.enrollDeadline = enrollDeadline; }

    public String getBannerUrl() { return bannerUrl; }
    public void setBannerUrl(String bannerUrl) { this.bannerUrl = bannerUrl; }

    public List<String> getGalleryUrls() { return galleryUrls; }
    public void setGalleryUrls(List<String> galleryUrls) { this.galleryUrls = galleryUrls; }

    public String getMusicType() { return musicType; }
    public void setMusicType(String musicType) { this.musicType = musicType; }

    public String getGenre() { return genre; }
    public void setGenre(String genre) { this.genre = genre; }

    public List<SpeakerDTO> getSpeakers() { return speakers; }
    public void setSpeakers(List<SpeakerDTO> speakers) { this.speakers = speakers; }

    public List<ScheduleDTO> getSchedules() { return schedules; }
    public void setSchedules(List<ScheduleDTO> schedules) { this.schedules = schedules; }

    public Map<LocalDate, List<ScheduleDTO>> getSchedulesByDay() { return schedulesByDay; }
    public void setSchedulesByDay(Map<LocalDate, List<ScheduleDTO>> schedulesByDay) { this.schedulesByDay = schedulesByDay; }

    public List<PlaceDTO> getPlaces() { return places; }
    public void setPlaces(List<PlaceDTO> places) { this.places = places; }

    // PHƯƠNG THỨC BẠN ĐANG THIẾU
    public List<TicketTypeDTO> getTicketTypes() { return ticketTypes; }
    public void setTicketTypes(List<TicketTypeDTO> ticketTypes) { this.ticketTypes = ticketTypes; }

    public OrganizationDTO getOrganization() { return organization; }
    public void setOrganization(OrganizationDTO organization) { this.organization = organization; }

    public EventStatus getStatus() { return status; }
    public void setStatus(EventStatus status) { this.status = status; }

    public EventType getEventType() { return eventType; }
    public void setEventType(EventType eventType) { this.eventType = eventType; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

}
