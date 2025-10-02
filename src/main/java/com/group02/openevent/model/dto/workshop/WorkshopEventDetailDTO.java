package com.group02.openevent.model.dto.workshop;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.group02.openevent.model.dto.PlaceDTO;
import com.group02.openevent.model.dto.ScheduleDTO;
import com.group02.openevent.model.dto.SpeakerDTO;
import com.group02.openevent.model.enums.EventType;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.util.List;

public class WorkshopEventDetailDTO {
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

    // Workshop specific fields
    private String materials;       // Tài liệu học tập
    private String prerequisites;   // Yêu cầu tiên quyết
    private String learningOutcomes; // Kết quả học tập
    private String certification;   // Chứng chỉ
    private String equipment;       // Thiết bị cần thiết
    private String skillLevel;      // Cấp độ kỹ năng (Beginner/Intermediate/Advanced)

    // Common fields
    private List<String> imageUrls;        // gallery, banner
    private List<SpeakerDTO> speakers;     // giảng viên, chuyên gia
    private List<ScheduleDTO> schedules;   // lịch trình workshop
    private List<PlaceDTO> places;         // địa điểm tổ chức

    // Constructors
    public WorkshopEventDetailDTO() {}

    public WorkshopEventDetailDTO(String description, String title, Integer capacity, 
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

    public WorkshopEventDetailDTO(String description, String title, Integer capacity, 
                                LocalDateTime startsAt, LocalDateTime endsAt, 
                                LocalDateTime createdAt, LocalDateTime updatedAt, 
                                EventType eventType, String benefits, 
                                String materials, String prerequisites, String learningOutcomes, 
                                String certification, String equipment, String skillLevel,
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
        this.materials = materials;
        this.prerequisites = prerequisites;
        this.learningOutcomes = learningOutcomes;
        this.certification = certification;
        this.equipment = equipment;
        this.skillLevel = skillLevel;
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

    public String getMaterials() { return materials; }
    public void setMaterials(String materials) { this.materials = materials; }

    public String getPrerequisites() { return prerequisites; }
    public void setPrerequisites(String prerequisites) { this.prerequisites = prerequisites; }

    public String getLearningOutcomes() { return learningOutcomes; }
    public void setLearningOutcomes(String learningOutcomes) { this.learningOutcomes = learningOutcomes; }

    public String getCertification() { return certification; }
    public void setCertification(String certification) { this.certification = certification; }

    public String getEquipment() { return equipment; }
    public void setEquipment(String equipment) { this.equipment = equipment; }

    public String getSkillLevel() { return skillLevel; }
    public void setSkillLevel(String skillLevel) { this.skillLevel = skillLevel; }

    public List<String> getImageUrls() { return imageUrls; }
    public void setImageUrls(List<String> imageUrls) { this.imageUrls = imageUrls; }

    public List<SpeakerDTO> getSpeakers() { return speakers; }
    public void setSpeakers(List<SpeakerDTO> speakers) { this.speakers = speakers; }

    public List<ScheduleDTO> getSchedules() { return schedules; }
    public void setSchedules(List<ScheduleDTO> schedules) { this.schedules = schedules; }

    public List<PlaceDTO> getPlaces() { return places; }
    public void setPlaces(List<PlaceDTO> places) { this.places = places; }
}
