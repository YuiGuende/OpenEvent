package com.group02.openevent.model.ticket;

import com.group02.openevent.model.event.Event;
import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "ticket_type")
public class TicketType {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ticket_type_id")
    private Long ticketTypeId;
    
    @Column(name = "name")
    private String name;
    
    @Column(name = "price", precision = 38, scale = 2)
    private BigDecimal price;
    
    @Column(name = "total_quantity")
    private Integer totalQuantity;
    
    @ManyToOne(optional = false)
    @JoinColumn(name = "event_id", nullable = false,
        foreignKey = @ForeignKey(name = "fk_tickettype_event"))
    private Event event;
    
    // Constructors
    public TicketType() {}
    
    public TicketType(String name, BigDecimal price, Integer totalQuantity, Event event) {
        this.name = name;
        this.price = price;
        this.totalQuantity = totalQuantity;
        this.event = event;
    }
    
    // Getters and Setters
    public Long getTicketTypeId() {
        return ticketTypeId;
    }
    
    public void setTicketTypeId(Long ticketTypeId) {
        this.ticketTypeId = ticketTypeId;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public BigDecimal getPrice() {
        return price;
    }
    
    public void setPrice(BigDecimal price) {
        this.price = price;
    }
    
    public Integer getTotalQuantity() {
        return totalQuantity;
    }
    
    public void setTotalQuantity(Integer totalQuantity) {
        this.totalQuantity = totalQuantity;
    }
    
    public Event getEvent() {
        return event;
    }
    
    public void setEvent(Event event) {
        this.event = event;
    }
    
    @Override
    public String toString() {
        return "TicketType{" +
                "ticketTypeId=" + ticketTypeId +
                ", name='" + name + '\'' +
                ", price=" + price +
                ", totalQuantity=" + totalQuantity +
                ", event=" + event +
                '}';
    }
}