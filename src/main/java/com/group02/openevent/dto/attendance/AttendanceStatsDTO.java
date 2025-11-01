package com.group02.openevent.dto.attendance;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceStatsDTO {
    private Long eventId;
    private String eventTitle;
    private long totalCheckedIn;
    private long totalCheckedOut;
    private long currentlyPresent;
    private long totalAttendees;
}



