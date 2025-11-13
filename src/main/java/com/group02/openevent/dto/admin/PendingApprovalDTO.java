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
public class PendingApprovalDTO {
    private Long requestId;
    private Long eventId;
    private String eventTitle;
    private String eventType;
    private String imageUrl;
    private String hostName;
    private String departmentName;
    private LocalDateTime createdAt;
    private Long daysPending;
    private String message;
    private String fileURL;
}

