package com.group02.openevent.ai.dto;

import com.group02.openevent.model.event.Event;
import com.group02.openevent.model.ticket.TicketType;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * DTO to hold pending order information during AI conversation
 */
@Data
public class PendingOrder {
    private Event event;
    private TicketType ticketType;
    private String participantName;
    private String participantEmail;
    private String participantPhone;
    private String participantOrganization;
    private String notes;
    private OrderStep currentStep;
    private LocalDateTime createdAt;
    
    public enum OrderStep {
        SELECT_EVENT,
        SELECT_TICKET_TYPE,
        PROVIDE_INFO,
        CONFIRM_ORDER
    }
    
    public PendingOrder() {
        this.currentStep = OrderStep.SELECT_EVENT;
        this.createdAt = LocalDateTime.now();
    }
    
    public boolean isComplete() {
        return event != null 
            && ticketType != null 
            && participantName != null && !participantName.isEmpty()
            && participantEmail != null && !participantEmail.isEmpty();
    }
    
    public String getMissingFields() {
        StringBuilder missing = new StringBuilder();
        
        if (event == null) missing.append("- Sự kiện\n");
        if (ticketType == null) missing.append("- Loại vé\n");
        if (participantName == null || participantName.isEmpty()) missing.append("- Tên người tham gia\n");
        if (participantEmail == null || participantEmail.isEmpty()) missing.append("- Email\n");
        
        return missing.toString();
    }
}

































