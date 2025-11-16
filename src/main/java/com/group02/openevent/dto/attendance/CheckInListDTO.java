package com.group02.openevent.dto.attendance;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CheckInListDTO {
    private Long attendanceId;
    private Long orderId; // Mã vé
    private String fullName;
    private String email;
    private String phone;
    private String organization;
    private LocalDateTime checkInTime;
    private LocalDateTime checkOutTime;
    private String status; // PENDING, CHECKED_IN, CHECKED_OUT
    private LocalDateTime createdAt;
}

