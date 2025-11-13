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
public class AttendanceStatsDTO {
    private Long eventId;
    private String eventTitle;
    private String eventType;
    private LocalDateTime startsAt;
    private Long totalRegistered;
    private Long checkedInCount;
    private Long checkedOutCount;
    private Long noShowCount;
    private Double attendanceRate;
    private Double checkInRate;
    private Double checkOutRate;
}

