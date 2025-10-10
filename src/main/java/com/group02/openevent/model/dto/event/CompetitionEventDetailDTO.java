package com.group02.openevent.model.dto.event;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.group02.openevent.model.dto.*;
import com.group02.openevent.model.enums.CompetitionFormat; // Cần import
import com.group02.openevent.model.enums.EventStatus;
import com.group02.openevent.model.enums.EventType;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
public class CompetitionEventDetailDTO {
    // ... (các trường chung)
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

    // Các trường đặc thù của Competition
    private String competitionType;
    private String rules;
    private String prizePool;
    private String eligibility;
    // ⭐ CẶP GETTER/SETTER BỊ THIẾU
    private CompetitionFormat format; // ⭐ TRƯỜNG BỊ THIẾU
    private String judgingCriteria;

    // Các danh sách liên quan
    private List<SpeakerDTO> speakers;
    private List<ScheduleDTO> schedules;
    private List<PlaceDTO> places;
    private List<TicketTypeDTO> ticketTypes;

    // Các trường khác
    private OrganizationDTO organization;
    private EventStatus status;
    private EventType eventType;
    private LocalDateTime createdAt;

    public CompetitionEventDetailDTO() {}

    // GETTERS AND SETTERS CHO TẤT CẢ CÁC TRƯỜNG

    public void setTitle(String title) { this.title = title; }

    public void setDescription(String description) { this.description = description; }

    public void setCapacity(Integer capacity) { this.capacity = capacity; }

    public void setBenefits(String benefits) { this.benefits = benefits; }

    public void setGuidelines(String guidelines) { this.guidelines = guidelines; }

    public void setVenueAddress(String venueAddress) { this.venueAddress = venueAddress; }

    public void setStartsAt(LocalDateTime startsAt) { this.startsAt = startsAt; }

    public void setEndsAt(LocalDateTime endsAt) { this.endsAt = endsAt; }

    public void setEnrollDeadline(LocalDateTime enrollDeadline) { this.enrollDeadline = enrollDeadline; }

    public void setBannerUrl(String bannerUrl) { this.bannerUrl = bannerUrl; }

    public void setGalleryUrls(List<String> galleryUrls) { this.galleryUrls = galleryUrls; }

    public void setCompetitionType(String competitionType) { this.competitionType = competitionType; }

    public void setRules(String rules) { this.rules = rules; }

    public void setPrizePool(String prizePool) { this.prizePool = prizePool; }

    public void setEligibility(String eligibility) { this.eligibility = eligibility; }

    public void setJudgingCriteria(String judgingCriteria) { this.judgingCriteria = judgingCriteria; }

    public void setSpeakers(List<SpeakerDTO> speakers) { this.speakers = speakers; }

    public void setSchedules(List<ScheduleDTO> schedules) { this.schedules = schedules; }

    public void setPlaces(List<PlaceDTO> places) { this.places = places; }

    public void setTicketTypes(List<TicketTypeDTO> ticketTypes) { this.ticketTypes = ticketTypes; }

    public void setOrganization(OrganizationDTO organization) { this.organization = organization; }

    public void setStatus(EventStatus status) { this.status = status; }

    public void setEventType(EventType eventType) { this.eventType = eventType; }

    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public void setFormat(CompetitionFormat format) { this.format = format; }
}