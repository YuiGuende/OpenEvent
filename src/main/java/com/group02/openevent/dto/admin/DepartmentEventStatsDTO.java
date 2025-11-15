package com.group02.openevent.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DepartmentEventStatsDTO {
    private Long departmentId;
    private String departmentName;
    private Long totalEvents;
    private Long activeEvents;
    private Long pendingApproval;
    private Long completedEvents;
    private Long totalRevenue;
    private Long totalParticipants;
    private Double averageRating;
}

