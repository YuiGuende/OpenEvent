package com.group02.openevent.model.ticket;

import com.group02.openevent.model.event.Event;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "ticket_type")
public class TicketType {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ticket_type_id")
    private Long ticketTypeId;

    @ManyToOne(optional = false)
    @JoinColumn(name = "event_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_tickettype_event"))
    private Event event;

    @Column(name = "name", length = 100, nullable = false)
    private String name;

    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "price", precision = 38, scale = 2, nullable = false)
    private BigDecimal price;

    @Column(name = "total_quantity", nullable = false)
    private Integer quantity;

    @Column(name = "sold_quantity", nullable = false)
    private Integer soldQuantity = 0;

    @Column(name = "start_sale_date")
    private LocalDateTime startSaleDate;

    @Column(name = "end_sale_date")
    private LocalDateTime endSaleDate;

    public Long getTicketTypeId() { return ticketTypeId; }
    public void setTicketTypeId(Long ticketTypeId) { this.ticketTypeId = ticketTypeId; }

    public Event getEvent() { return event; }
    public void setEvent(Event event) { this.event = event; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public Integer getSoldQuantity() { return soldQuantity; }
    public void setSoldQuantity(Integer soldQuantity) { this.soldQuantity = soldQuantity; }

    public LocalDateTime getStartSaleDate() { return startSaleDate; }
    public void setStartSaleDate(LocalDateTime startSaleDate) { this.startSaleDate = startSaleDate; }

    public LocalDateTime getEndSaleDate() { return endSaleDate; }
    public void setEndSaleDate(LocalDateTime endSaleDate) { this.endSaleDate = endSaleDate; }
}


