package com.group02.openevent.dto.home;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TopStudentDTO {
    private Long customerId;
    private String name;
    private String email;
    private String imageUrl;
    private String organization;
    private Integer points; // Số điểm của sinh viên
    private Integer rank; // Thứ hạng (1, 2, 3)
}

