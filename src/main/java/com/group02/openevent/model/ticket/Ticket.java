package com.group02.openevent.model.ticket;

import com.group02.openevent.model.user.User;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "ticket")
public class Ticket {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ticket_id")
    private Long ticketId;
    
    @ManyToOne(optional = false)
    @JoinColumn(name = "ticket_type_id", nullable = false,
        foreignKey = @ForeignKey(name = "fk_ticket_type"))
    private TicketType ticketType;
    
    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false,
        foreignKey = @ForeignKey(name = "fk_ticket_user_old"))
    private User user;
    
    @Column(name = "purchase_date")
    private LocalDateTime purchaseDate = LocalDateTime.now();
    
    @PrePersist
    protected void onCreate() {
        this.purchaseDate = LocalDateTime.now();
    }
    
    // Constructors
    public Ticket() {}
    
    public Ticket(TicketType ticketType, User user) {
        this.ticketType = ticketType;
        this.user = user;
        this.purchaseDate = LocalDateTime.now();
    }
    
    // Getters and Setters
    public Long getTicketId() {
        return ticketId;
    }
    
    public void setTicketId(Long ticketId) {
        this.ticketId = ticketId;
    }
    
    public TicketType getTicketType() {
        return ticketType;
    }
    
    public void setTicketType(TicketType ticketType) {
        this.ticketType = ticketType;
    }
    
    public User getUser() {
        return user;
    }
    
    public void setUser(User user) {
        this.user = user;
    }
    
    public LocalDateTime getPurchaseDate() {
        return purchaseDate;
    }
    
    public void setPurchaseDate(LocalDateTime purchaseDate) {
        this.purchaseDate = purchaseDate;
    }
    
    @Override
    public String toString() {
        return "Ticket{" +
                "ticketId=" + ticketId +
                ", ticketType=" + ticketType +
                ", user=" + user +
                ", purchaseDate=" + purchaseDate +
                '}';
    }
}