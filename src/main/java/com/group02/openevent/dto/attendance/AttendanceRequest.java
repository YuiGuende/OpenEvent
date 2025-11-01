package com.group02.openevent.dto.attendance;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceRequest {
    private String fullName;
    private String email;
    private String phone;
    private String organization;
}



