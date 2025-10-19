package com.group02.openevent.dto.notification;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

/**
 * DTO for displaying the request form with event details and available departments
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RequestFormDTO {
    private String eventName;
    private Long eventId;
    private List<DepartmentDTO> departments;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DepartmentDTO {
        private Long id;
        private String name;
    }
}
