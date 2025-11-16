package com.group02.openevent.dto.chat;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageDTO {
    private Long roomId;
    private Long eventId;
    private Long messageId;
    private Long senderUserId;
    private String senderName;  // Tên người gửi để hiển thị
    private Long recipientUserId;
    private String body;
    private Instant timestamp;
}


