package com.group02.openevent.model.ticket;
import com.fasterxml.jackson.annotation.JsonIgnore;
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
    @JsonIgnore
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
    // NOTE: 'sale' field in DB stores PERCENTAGE (0-100), not absolute amount
    public BigDecimal getFinalPrice() {
        if (sale == null || sale.compareTo(BigDecimal.ZERO) <= 0 || price == null) {
            return price;
        }
        // Convert percentage to absolute discount amount
        BigDecimal discountAmount = price.multiply(sale).divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
        return price.subtract(discountAmount);
    }
    
    // Get absolute discount amount from percentage
    public BigDecimal getDiscountAmount() {
        if (sale == null || sale.compareTo(BigDecimal.ZERO) <= 0 || price == null) {
            return BigDecimal.ZERO;
        }
        return price.multiply(sale).divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
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

    public boolean canPurchase() {
        // For now, ignore sale period for testing - only check availability and quantity
        return isAvailable() && 1 <= getAvailableQuantity();
    }

    public synchronized void increaseSoldQuantity() {
        if (1 + soldQuantity > totalQuantity) {
            throw new IllegalArgumentException("Không đủ vé còn lại");
        }
        this.soldQuantity += 1;
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