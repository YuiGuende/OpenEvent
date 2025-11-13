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
public class PointsTrackingDTO {
    private Long eventId;
    private String eventTitle;
    private String eventType;
    private Integer pointsAwarded;
    private Long studentsEarnedPoints;
    private Long totalParticipants;
    private Double pointsDistributionRate;
    private String learningObjectives;
    private LocalDateTime startsAt;
}

