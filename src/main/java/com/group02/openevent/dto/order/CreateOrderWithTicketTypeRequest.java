package com.group02.openevent.dto.order;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;

public class CreateOrderWithTicketTypeRequest {
    
    @NotNull(message = "Event ID is required")
    private Long eventId;
    
    private Long userId;
    
    private String participantName;
    private String participantEmail;
    private String participantPhone;
    private String participantOrganization;
    private String notes;
    private String voucherCode;
    
//    @Valid
//    @NotNull(message = "Order items are required")
//    private List<OrderItemRequest> orderItems;

    @NotNull(message = "Ticket type ID is required")
    private Long ticketTypeId;

    // Constructors
    public CreateOrderWithTicketTypeRequest() {}

    public CreateOrderWithTicketTypeRequest(Long eventId, Long userId, Long ticketTypeId) {
        this.eventId = eventId;
        this.userId = userId;
        this.ticketTypeId = ticketTypeId;
    }

    public Long getEventId() {
        return eventId;
    }

    public void setEventId(Long eventId) {
        this.eventId = eventId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getParticipantName() {
        return participantName;
    }

    public void setParticipantName(String participantName) {
        this.participantName = participantName;
    }

    public String getParticipantEmail() {
        return participantEmail;
    }

    public void setParticipantEmail(String participantEmail) {
        this.participantEmail = participantEmail;
    }

    public String getParticipantPhone() {
        return participantPhone;
    }

    public void setParticipantPhone(String participantPhone) {
        this.participantPhone = participantPhone;
    }

    public String getParticipantOrganization() {
        return participantOrganization;
    }

    public void setParticipantOrganization(String participantOrganization) {
        this.participantOrganization = participantOrganization;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getVoucherCode() {
        return voucherCode;
    }

    public void setVoucherCode(String voucherCode) {
        this.voucherCode = voucherCode;
    }

    public Long getTicketTypeId() {
        return ticketTypeId;
    }

    public void setTicketTypeId(Long ticketTypeId) {
        this.ticketTypeId = ticketTypeId;
    }

    // Inner class for order items
    public static class OrderItemRequest {
        
        @NotNull(message = "Ticket type ID is required")
        private Long ticketTypeId;
        
        @NotNull(message = "Quantity is required")
        @Positive(message = "Quantity must be positive")
        private Integer quantity;

        // Constructors
        public OrderItemRequest() {}

        public OrderItemRequest(Long ticketTypeId, Integer quantity) {
            this.ticketTypeId = ticketTypeId;
            this.quantity = quantity;
        }

        // Getters and Setters
        public Long getTicketTypeId() {
            return ticketTypeId;
        }

        public void setTicketTypeId(Long ticketTypeId) {
            this.ticketTypeId = ticketTypeId;
        }

        public Integer getQuantity() {
            return quantity;
        }

        public void setQuantity(Integer quantity) {
            this.quantity = quantity;
        }

        @Override
        public String toString() {
            return "OrderItemRequest{" +
                    "ticketTypeId=" + ticketTypeId +
                    ", quantity=" + quantity +
                    '}';
        }
    }


}
