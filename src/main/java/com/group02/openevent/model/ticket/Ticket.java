package com.group02.openevent.model.ticket;

import com.group02.openevent.model.user.User;
import com.group02.openevent.model.event.Event;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.math.BigDecimal;

@Entity
@Table(name = "tickets")
public class Ticket {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ticket_id")
    private Long ticketId;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false,
        foreignKey = @ForeignKey(name = "fk_ticket_user"))
    private User user;

    @ManyToOne(optional = false)
    @JoinColumn(name = "event_id", nullable = false,
        foreignKey = @ForeignKey(name = "fk_ticket_event"))
    private Event event;

    @Column(name = "ticket_code", length = 50, unique = true, nullable = false)
    private String ticketCode;

    @Column(name = "price", precision = 10, scale = 2, nullable = false)
    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private TicketStatus status = TicketStatus.PENDING;

    @Column(name = "description", length = 500)
    private String description;

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

    @Column(name = "ticket_type_name", length = 255)
    private String ticketTypeName;

    @Column(name = "purchase_date", nullable = false)
    private LocalDateTime purchaseDate = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Constructors
    public Ticket() {}

    public Ticket(User user, Event event, String ticketCode, BigDecimal price) {
        this.user = user;
        this.event = event;
        this.ticketCode = ticketCode;
        this.price = price;
        this.status = TicketStatus.PENDING;
        this.purchaseDate = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getTicketId() {
        return ticketId;
    }

    public void setTicketId(Long ticketId) {
        this.ticketId = ticketId;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Event getEvent() {
        return event;
    }

    public void setEvent(Event event) {
        this.event = event;
    }

    public String getTicketCode() {
        return ticketCode;
    }

    public void setTicketCode(String ticketCode) {
        this.ticketCode = ticketCode;
    }


    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public TicketStatus getStatus() {
        return status;
    }

    public void setStatus(TicketStatus status) {
        this.status = status;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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

    public String getTicketTypeName() {
        return ticketTypeName;
    }

    public void setTicketTypeName(String ticketTypeName) {
        this.ticketTypeName = ticketTypeName;
    }

    public LocalDateTime getPurchaseDate() {
        return purchaseDate;
    }

    public void setPurchaseDate(LocalDateTime purchaseDate) {
        this.purchaseDate = purchaseDate;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return "Ticket{" +
                "ticketId=" + ticketId +
                ", ticketCode='" + ticketCode + '\'' +
                ", price=" + price +
                ", status=" + status +
                ", ticketTypeName='" + ticketTypeName + '\'' +
                ", purchaseDate=" + purchaseDate +
                '}';
    }
}