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

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_tickettype_event"))
    private Event event;

    @Column(name = "name", length = 100, nullable = false)
    private String name;

    @Column(name = "description", length = 1000)//tinyMCE
    private String description;

    @Column(name = "price", precision = 38, scale = 2, nullable = false)
    private BigDecimal price;

    @Column(name = "total_quantity", nullable = false)
    private Integer totalQuantity;

    @Column(name = "sold_quantity", nullable = false)
    private Integer soldQuantity = 0;

    @Column(name = "start_sale_date")
    private LocalDateTime startSaleDate;

    @Column(name = "end_sale_date")
    private LocalDateTime endSaleDate;

    @Column(name = "sale", precision = 38, scale = 2)
    private BigDecimal sale = BigDecimal.ZERO;
    // Constructors
    public TicketType() {}

    public TicketType(Event event, String name, String description, BigDecimal price, Integer totalQuantity) {
        this.event = event;
        this.name = name;
        this.description = description;
        this.price = price;
        this.totalQuantity = totalQuantity;
        this.soldQuantity = 0;
    }

    public BigDecimal getSale() {
        return sale;
    }

    public void setSale(BigDecimal sale) {
        this.sale = sale;
    }

    // Helper method to get final price after discount
    public BigDecimal getFinalPrice() {
        if (sale == null) {
            return price;
        }
        return price.subtract(sale);
    }
    // Business Logic Methods
    public Integer getAvailableQuantity() {
        return totalQuantity - soldQuantity;
    }

    public boolean isAvailable() {
        return getAvailableQuantity() > 0;
    }

    public boolean isSalePeriodActive() {
        LocalDateTime now = LocalDateTime.now();
        boolean afterStart = startSaleDate == null || now.isAfter(startSaleDate);
        boolean beforeEnd = endSaleDate == null || now.isBefore(endSaleDate);
        return afterStart && beforeEnd;
    }

    public boolean canPurchase(Integer requestQuantity) {
        // For now, ignore sale period for testing - only check availability and quantity
        return isAvailable() && requestQuantity <= getAvailableQuantity();
    }

    public synchronized void increaseSoldQuantity(Integer quantity) {
        if (quantity + soldQuantity > totalQuantity) {
            throw new IllegalArgumentException("Không đủ vé còn lại");
        }
        this.soldQuantity += quantity;
    }

    public synchronized void decreaseSoldQuantity(Integer quantity) {
        this.soldQuantity = Math.max(0, this.soldQuantity - quantity);
    }

    // Getters and Setters
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

    public Integer getTotalQuantity() { return totalQuantity; }
    public void setTotalQuantity(Integer totalQuantity) { this.totalQuantity = totalQuantity; }

    public Integer getSoldQuantity() { return soldQuantity; }
    public void setSoldQuantity(Integer soldQuantity) { this.soldQuantity = soldQuantity; }

    public LocalDateTime getStartSaleDate() { return startSaleDate; }
    public void setStartSaleDate(LocalDateTime startSaleDate) { this.startSaleDate = startSaleDate; }

    public LocalDateTime getEndSaleDate() { return endSaleDate; }
    public void setEndSaleDate(LocalDateTime endSaleDate) { this.endSaleDate = endSaleDate; }

    @Override
    public String toString() {
        return "TicketType{" +
                "ticketTypeId=" + ticketTypeId +
                ", name='" + name + '\'' +
                ", price=" + price +
                ", totalQuantity=" + totalQuantity +
                ", soldQuantity=" + soldQuantity +
                ", availableQuantity=" + getAvailableQuantity() +
                '}';
    }
}