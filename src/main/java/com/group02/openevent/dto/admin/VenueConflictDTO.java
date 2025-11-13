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
public class VenueConflictDTO {
    private Long event1Id;
    private String event1Title;
    private LocalDateTime event1Start;
    private LocalDateTime event1End;
    private Long event2Id;
    private String event2Title;
    private LocalDateTime event2Start;
    private LocalDateTime event2End;
    private String venueName;
    private String conflictSeverity;
    private Long overlapMinutes;
}

