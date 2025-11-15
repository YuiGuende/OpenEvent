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
public class UpcomingEventDTO {
    private Long eventId;
    private String title;
    private String eventType;
    private String status;
    private LocalDateTime startsAt;
    private LocalDateTime endsAt;
    private String venue;
    private Long registeredCount;
    private Integer capacity;
    private Double fillRate;
    private String departmentName;
    private String hostName;
    private Long daysUntil;
}

