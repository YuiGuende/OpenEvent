package com.group02.openevent.model.user;

import com.group02.openevent.model.event.Event;
import com.group02.openevent.model.organization.Organization;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
@Getter
@Setter
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
            foreignKey = @ForeignKey(name = "fk_host_organization"))
    private Organization organization;

    @OneToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_host_customer_jpa"))
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
