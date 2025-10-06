package com.group02.openevent.model.dto.competition;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.group02.openevent.model.dto.PlaceDTO;
import com.group02.openevent.model.dto.ScheduleDTO;
import com.group02.openevent.model.dto.SpeakerDTO;
import com.group02.openevent.model.enums.CompetitionFormat;
import com.group02.openevent.model.enums.EventType;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.util.List;

public class CompetitionEventDetailDTO {
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

    // 🔹 mở rộng thêm
    private List<String> imageUrls;        // gallery, banner
    private List<SpeakerDTO> speakers;     // danh sách nghệ sĩ
    private List<ScheduleDTO> schedules;   // lịch biểu
    private List<PlaceDTO> places;         // địa điểm
    private String venueAddress;           // địa chỉ venue
    private String guidelines;             // hướng dẫn sự kiện


    //mở rộng thêm của competition
    private String eligibility;               // điều kiện tham gia
    private CompetitionFormat format;         // SOLO hoặc TEAM
    private String judgingCriteria;           // tiêu chí chấm điểm


    public String getEligibility() {
        return eligibility;
    }

    public void setEligibility(String eligibility) {
        this.eligibility = eligibility;
    }

    public CompetitionFormat getFormat() {
        return format;
    }

    public void setFormat(CompetitionFormat format) {
        this.format = format;
    }

    public String getJudgingCriteria() {
        return judgingCriteria;
    }

    public void setJudgingCriteria(String judgingCriteria) {
        this.judgingCriteria = judgingCriteria;
    }

    public CompetitionEventDetailDTO() {
    }


    //full constructure
    public CompetitionEventDetailDTO(String description, String title, Integer capacity, LocalDateTime startsAt, LocalDateTime endsAt, LocalDateTime createdAt, EventType eventType, String benefits, List<String> imageUrls, List<SpeakerDTO> speakers, List<ScheduleDTO> schedules, List<PlaceDTO> places, String venueAddress, String guidelines, String eligibility, CompetitionFormat format, String judgingCriteria) {
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
        this.eligibility = eligibility;
        this.format = format;
        this.judgingCriteria = judgingCriteria;
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

    // Default constructor
    public CompetitionEventDetailDTO(String description, String title, Integer capacity, LocalDateTime startsAt, LocalDateTime endsAt, LocalDateTime createdAt, Object o, EventType eventType, String benefits, Object object, Object o1, Object object1, Object o2, String venueAddress, String guidelines) {
        this.description = description;
        this.title = title;
        this.capacity = capacity;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
        this.createdAt = createdAt;
        this.eventType = eventType;
        this.benefits = benefits;
        this.venueAddress = venueAddress;
        this.guidelines = guidelines;
    }

    public CompetitionEventDetailDTO(String description, String title, Integer capacity, LocalDateTime startsAt, LocalDateTime endsAt, LocalDateTime createdAt, EventType eventType, String benefits, List<String> imageUrls, List<SpeakerDTO> speakers, List<ScheduleDTO> schedules, List<PlaceDTO> places, String venueAddress, String guidelines) {
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
    }

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
}
