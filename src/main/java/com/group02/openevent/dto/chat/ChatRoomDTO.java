package com.group02.openevent.dto.chat;

public record ChatRoomDTO(
        Long id,
        String createdAt,
        UserSummaryDTO host,
        UserSummaryDTO volunteer,  // For HOST_DEPARTMENT: department, For HOST_VOLUNTEERS: null
        String roomType,  // "HOST_DEPARTMENT" or "HOST_VOLUNTEERS"
        Long eventId,  // For HOST_VOLUNTEERS room
        String eventTitle  // For HOST_VOLUNTEERS room
) {}

