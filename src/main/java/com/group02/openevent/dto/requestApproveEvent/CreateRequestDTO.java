package com.group02.openevent.dto.requestApproveEvent;


import com.group02.openevent.model.request.RequestType;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateRequestDTO {
    
    private Long senderId;
    private Long receiverId;
    private RequestType type;
    private Long eventId;
    private String targetUrl;
    private Long orderId;
    private String message;
    private String fileURL;
}
