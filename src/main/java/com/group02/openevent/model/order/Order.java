package com.group02.openevent.model.order;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.group02.openevent.model.event.Event;
import com.group02.openevent.model.user.Customer;
import com.group02.openevent.model.ticket.TicketType;
import com.group02.openevent.model.voucher.Voucher;
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

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_order_customer"))
    @JsonIgnoreProperties({"orders", "passwordHash", "account"})
    private Customer customer;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_order_event"))
    @JsonIgnoreProperties({"orders", "ticketTypes", "eventImages", "host"})
    private Event event;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_type_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_order_tickettype"))
    @JsonIgnoreProperties({"event", "orders"})
    private TicketType ticketType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private OrderStatus status = OrderStatus.PENDING;

    // Pricing fields
    @Column(name = "original_price", precision = 10, scale = 2, nullable = false)
    private BigDecimal originalPrice;

    @Column(name = "host_discount_percent", precision = 5, scale = 2)
    private BigDecimal hostDiscountPercent = BigDecimal.ZERO;

    @Column(name = "host_discount_amount", precision = 10, scale = 2)
    private BigDecimal hostDiscountAmount = BigDecimal.ZERO;

    @Column(name = "voucher_discount_amount", precision = 10, scale = 2)
    private BigDecimal voucherDiscountAmount = BigDecimal.ZERO;

    @Column(name = "total_amount", precision = 10, scale = 2, nullable = false)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    // Voucher information
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "voucher_id")
    @JsonIgnoreProperties({"voucherUsages"})
    private Voucher voucher;

    @Column(name = "voucher_code", length = 20)
    private String voucherCode;

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
            this.originalPrice = ticketType.getPrice();
            
            // Calculate host discount amount
            if (hostDiscountPercent != null && hostDiscountPercent.compareTo(BigDecimal.ZERO) > 0) {
                this.hostDiscountAmount = originalPrice.multiply(hostDiscountPercent.divide(new BigDecimal("100")));
            }
            
            // Calculate price after discounts
            BigDecimal priceAfterDiscounts = originalPrice.subtract(hostDiscountAmount).subtract(voucherDiscountAmount);
            
            // Ensure price after discounts is not negative
            if (priceAfterDiscounts.compareTo(BigDecimal.ZERO) < 0) {
                priceAfterDiscounts = BigDecimal.ZERO;
            }
            
            // Calculate VAT (10%) on price after discounts
            BigDecimal vat = priceAfterDiscounts.multiply(new BigDecimal("0.10"));
            
            // Final total amount = price after discounts + VAT
            this.totalAmount = priceAfterDiscounts.add(vat);
            
            
        } else {
            this.originalPrice = BigDecimal.ZERO;
            this.totalAmount = BigDecimal.ZERO;
        }
    }

    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }

    public Customer getCustomer() { return customer; }
    public void setCustomer(Customer customer) { this.customer = customer; }

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

    // New getters and setters for pricing fields
    public BigDecimal getOriginalPrice() { return originalPrice; }
    public void setOriginalPrice(BigDecimal originalPrice) { this.originalPrice = originalPrice; }

    public BigDecimal getHostDiscountPercent() { return hostDiscountPercent; }
    public void setHostDiscountPercent(BigDecimal hostDiscountPercent) { 
        this.hostDiscountPercent = hostDiscountPercent; 
        calculateTotalAmount();
    }

    public BigDecimal getHostDiscountAmount() { return hostDiscountAmount; }
    public void setHostDiscountAmount(BigDecimal hostDiscountAmount) { this.hostDiscountAmount = hostDiscountAmount; }

    public BigDecimal getVoucherDiscountAmount() { return voucherDiscountAmount; }
    public void setVoucherDiscountAmount(BigDecimal voucherDiscountAmount) { 
        this.voucherDiscountAmount = voucherDiscountAmount; 
        calculateTotalAmount();
    }

    public Voucher getVoucher() { return voucher; }
    public void setVoucher(Voucher voucher) { this.voucher = voucher; }

    public String getVoucherCode() { return voucherCode; }
    public void setVoucherCode(String voucherCode) { this.voucherCode = voucherCode; }
}
