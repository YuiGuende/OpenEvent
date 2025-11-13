package com.group02.openevent.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventStatusDTO {
    private Long eventId;
    private String title;
    private String eventType;
    private String status;
    private String departmentName;
    private String hostName;
    private LocalDateTime startsAt;
    private LocalDateTime endsAt;
    private LocalDateTime createdAt;
    private Integer capacity;
    private Long registeredCount;
    private Long ticketsSold;
    private Long revenue;
    private Double attendanceRate;
}

