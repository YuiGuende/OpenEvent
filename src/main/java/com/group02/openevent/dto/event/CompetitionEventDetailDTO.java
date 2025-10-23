package com.group02.openevent.dto.event;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.group02.openevent.dto.*;
import com.group02.openevent.dto.ticket.TicketTypeDTO;
import com.group02.openevent.model.enums.CompetitionFormat;
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
public class CompetitionEventDetailDTO {
    private Long id;
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

    // ⭐ CÁC TRƯỜNG ĐẶC THÙ CỦA COMPETITION EVENT
    private String competitionType;
    private String rules;
    private String prizePool;
    private String eligibility;
    private CompetitionFormat format;
    private String judgingCriteria;

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
    public CompetitionEventDetailDTO() {}
}