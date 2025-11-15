package com.group02.openevent.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserListDTO {
    private Long userId;
    private String userName;
    private String email;
    private String role; // CUSTOMER, HOST, ADMIN
    private String status; // ACTIVE, INACTIVE, SUSPENDED
    private LocalDateTime registrationDate;
    private LocalDateTime lastActivityDate;
    private Long totalEvents;
    private Long totalOrders;
    private Long totalTickets;
    private Long totalSpent;
    private Long totalPoints;
    private Integer feedbackCount;
}

