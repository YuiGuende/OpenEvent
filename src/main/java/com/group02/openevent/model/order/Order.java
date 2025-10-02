package com.group02.openevent.model.order;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.group02.openevent.model.event.Event;
import com.group02.openevent.model.user.User;
import com.group02.openevent.model.ticket.TicketType;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_id")
    private Long orderId;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_order_user"))
    @JsonIgnoreProperties({"orders", "passwordHash", "account"})
    private User user;

    @ManyToOne(optional = false)
    @JoinColumn(name = "event_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_order_event"))
    @JsonIgnoreProperties({"orders", "ticketTypes", "eventImages", "host"})
    private Event event;

    @ManyToOne(optional = false)
    @JoinColumn(name = "ticket_type_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_order_tickettype"))
    @JsonIgnoreProperties({"event", "orders"})
    private TicketType ticketType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private OrderStatus status = OrderStatus.PENDING;

    @Column(name = "total_amount", precision = 10, scale = 2, nullable = false)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(name = "participant_name", length = 100)
    private String participantName;

    @Column(name = "participant_email", length = 100)
    private String participantEmail;

    @Column(name = "participant_phone", length = 20)
    private String participantPhone;

    @Column(name = "participant_organization", length = 150)
    private String participantOrganization;

    @Column(name = "notes", length = 1000)
    private String notes;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Business Logic Methods
    public void calculateTotalAmount() {
        if (ticketType != null) {
            this.totalAmount = ticketType.getPrice();
        } else {
            this.totalAmount = BigDecimal.ZERO;
        }
    }

    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public Event getEvent() { return event; }
    public void setEvent(Event event) { this.event = event; }

    public TicketType getTicketType() { return ticketType; }
    public void setTicketType(TicketType ticketType) { 
        this.ticketType = ticketType; 
        calculateTotalAmount();
    }

    public OrderStatus getStatus() { return status; }
    public void setStatus(OrderStatus status) { this.status = status; }

    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }

    public String getParticipantName() { return participantName; }
    public void setParticipantName(String participantName) { this.participantName = participantName; }

    public String getParticipantEmail() { return participantEmail; }
    public void setParticipantEmail(String participantEmail) { this.participantEmail = participantEmail; }

    public String getParticipantPhone() { return participantPhone; }
    public void setParticipantPhone(String participantPhone) { this.participantPhone = participantPhone; }

    public String getParticipantOrganization() { return participantOrganization; }
    public void setParticipantOrganization(String participantOrganization) { this.participantOrganization = participantOrganization; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
