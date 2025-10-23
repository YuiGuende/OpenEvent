package com.group02.openevent.model.user;

import com.group02.openevent.model.event.Event;
import com.group02.openevent.model.organization.Organization;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "hosts")
public class Host {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "host_id")
    private Long id;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organize_id",
            foreignKey = @ForeignKey(name = "fk_host_org"))
    private Organization organization;

    // Removed bidirectional mapping to prevent circular reference
    // Event already has @ManyToOne mapping to Host via host_id
    // This was causing duplicate rows in Hibernate

    @OneToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_host_customer"))
    private Customer customer;

    @Column(name = "host_discount_percent", precision = 5, scale = 2)
    private BigDecimal hostDiscountPercent = BigDecimal.ZERO;

    @Column(name = "description")
    private String description;

    // Host có thể trực tiếp tạo nhiều sự kiện
    @OneToMany(mappedBy = "host", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Event> events;

    public Host() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Organization getOrganization() {
        return organization;
    }

    public void setOrganization(Organization organization) {
        this.organization = organization;
    }

    // Removed getEvent() and setEvent() methods
    // Event can access Host via host_id, no need for bidirectional mapping

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }

    public List<Event> getEvents() {
        return events;
    }

    public void setEvents(List<Event> events) {
        this.events = events;
    }

    public BigDecimal getHostDiscountPercent() {
        return hostDiscountPercent;
    }

    public void setHostDiscountPercent(BigDecimal hostDiscountPercent) {
        this.hostDiscountPercent = hostDiscountPercent;
    }

    // Method to get host name from customer or organization
    public String getHostName() {
        if (customer != null && customer.getAccount() != null) {
            return customer.getAccount().getEmail();
        }
        if (organization != null && organization.getOrgName() != null) {
            return organization.getOrgName();
        }
        return "Unknown Host";
    }

    @Override
    public String toString() {
        return "Host{" +
                "id=" + id +
                ", createdAt=" + createdAt +
                ", hostDiscountPercent=" + hostDiscountPercent +
                '}';
    }
}
