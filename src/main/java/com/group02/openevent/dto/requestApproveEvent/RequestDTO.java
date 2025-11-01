package com.group02.openevent.dto.requestApproveEvent;


import com.group02.openevent.model.request.RequestStatus;
import com.group02.openevent.model.request.RequestType;
import lombok.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequestDTO {
    
    private Long requestId;
    private Long senderId;
    private String senderName;
    private Long receiverId;
    private String receiverName;
    private RequestType type;
    private Long eventId;
    private String eventTitle;
    private String targetUrl;
    private Long orderId;
    private String message;
    private String fileURL;
    private String responseMessage;
    private RequestStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Computed fields for display
    public String getFormattedCreatedAt() {
        if (createdAt == null) return "";
        return createdAt.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
    }
    
    public String getFormattedUpdatedAt() {
        if (updatedAt == null) return "";
        return updatedAt.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
    }
    
    public String getStatusBadgeClass() {
        if (status == null) return "badge-secondary";
        return switch (status) {
            case PENDING -> "badge-warning";
            case APPROVED -> "badge-success";
            case REJECTED -> "badge-danger";
        };
    }
    
    public String getTypeBadgeClass() {
        if (type == null) return "badge-secondary";
        return switch (type) {
            case EVENT_APPROVAL -> "badge-primary";
            case REFUND_TICKET -> "badge-info";
            case REPORT -> "badge-danger";
            case OTHER -> "badge-secondary";
        };
    }
}
