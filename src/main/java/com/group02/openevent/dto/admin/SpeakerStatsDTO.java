package com.group02.openevent.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SpeakerStatsDTO {
    private Long speakerId;
    private String speakerName;
    private String role;
    private String imageUrl;
    private Long eventsCount;
    private Long totalParticipants;
    private Double averageRating;
    private Long totalRevenue;
}

