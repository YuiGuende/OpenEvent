package com.group02.openevent.model.dto.competition;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.group02.openevent.model.dto.PlaceDTO;
import com.group02.openevent.model.dto.ScheduleDTO;
import com.group02.openevent.model.dto.SpeakerDTO;
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
    private LocalDateTime updatedAt;
    private EventType eventType;
    private String benefits;

    // Competition specific fields
    private String prize;           // Giải thưởng
    private String rules;           // Luật thi đấu
    private String requirements;    // Yêu cầu tham gia
    private String registrationFee; // Lệ phí đăng ký
    private String maxParticipants; // Số lượng thí sinh tối đa

    // Common fields
    private List<String> imageUrls;        // gallery, banner
    private List<SpeakerDTO> speakers;     // ban giám khảo, MC
    private List<ScheduleDTO> schedules;   // lịch biểu thi đấu
    private List<PlaceDTO> places;         // địa điểm thi

    // Constructors
    public CompetitionEventDetailDTO() {}

    public CompetitionEventDetailDTO(String description, String title, Integer capacity, 
                                   LocalDateTime startsAt, LocalDateTime endsAt, 
                                   LocalDateTime createdAt, LocalDateTime updatedAt, 
                                   EventType eventType, String benefits) {
        this.description = description;
        this.title = title;
        this.capacity = capacity;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.eventType = eventType;
        this.benefits = benefits;
    }

    public CompetitionEventDetailDTO(String description, String title, Integer capacity, 
                                   LocalDateTime startsAt, LocalDateTime endsAt, 
                                   LocalDateTime createdAt, LocalDateTime updatedAt, 
                                   EventType eventType, String benefits, 
                                   String prize, String rules, String requirements, 
                                   String registrationFee, String maxParticipants,
                                   List<String> imageUrls, List<SpeakerDTO> speakers, 
                                   List<ScheduleDTO> schedules, List<PlaceDTO> places) {
        this.description = description;
        this.title = title;
        this.capacity = capacity;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.eventType = eventType;
        this.benefits = benefits;
        this.prize = prize;
        this.rules = rules;
        this.requirements = requirements;
        this.registrationFee = registrationFee;
        this.maxParticipants = maxParticipants;
        this.imageUrls = imageUrls;
        this.speakers = speakers;
        this.schedules = schedules;
        this.places = places;
    }

    // Getters and Setters
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public Integer getCapacity() { return capacity; }
    public void setCapacity(Integer capacity) { this.capacity = capacity; }

    public LocalDateTime getStartsAt() { return startsAt; }
    public void setStartsAt(LocalDateTime startsAt) { this.startsAt = startsAt; }

    public LocalDateTime getEndsAt() { return endsAt; }
    public void setEndsAt(LocalDateTime endsAt) { this.endsAt = endsAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public EventType getEventType() { return eventType; }
    public void setEventType(EventType eventType) { this.eventType = eventType; }

    public String getBenefits() { return benefits; }
    public void setBenefits(String benefits) { this.benefits = benefits; }

    public String getPrize() { return prize; }
    public void setPrize(String prize) { this.prize = prize; }

    public String getRules() { return rules; }
    public void setRules(String rules) { this.rules = rules; }

    public String getRequirements() { return requirements; }
    public void setRequirements(String requirements) { this.requirements = requirements; }

    public String getRegistrationFee() { return registrationFee; }
    public void setRegistrationFee(String registrationFee) { this.registrationFee = registrationFee; }

    public String getMaxParticipants() { return maxParticipants; }
    public void setMaxParticipants(String maxParticipants) { this.maxParticipants = maxParticipants; }

    public List<String> getImageUrls() { return imageUrls; }
    public void setImageUrls(List<String> imageUrls) { this.imageUrls = imageUrls; }

    public List<SpeakerDTO> getSpeakers() { return speakers; }
    public void setSpeakers(List<SpeakerDTO> speakers) { this.speakers = speakers; }

    public List<ScheduleDTO> getSchedules() { return schedules; }
    public void setSchedules(List<ScheduleDTO> schedules) { this.schedules = schedules; }

    public List<PlaceDTO> getPlaces() { return places; }
    public void setPlaces(List<PlaceDTO> places) { this.places = places; }
}
