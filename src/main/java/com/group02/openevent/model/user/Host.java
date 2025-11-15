package com.group02.openevent.model.user;

import com.group02.openevent.model.event.Event;
import com.group02.openevent.model.organization.Organization;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

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
    @JoinColumn(name = "organize_id")
    private Organization organization;

    @OneToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

//    @OneToOne(optional = true, fetch = FetchType.LAZY)
//    @JoinColumn(name = "customer_id", nullable = true)
//    private Customer customer;

    @Column(name = "host_discount_percent", precision = 5, scale = 2)
    private BigDecimal hostDiscountPercent = BigDecimal.ZERO;

    @Column(name = "description")
    private String description;

    // Host có thể trực tiếp tạo nhiều sự kiện
    @OneToMany(mappedBy = "host", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @org.hibernate.annotations.BatchSize(size = 30)
    private List<Event> events;

    public Host() {
    }

    // Method to get host name from user, customer or organization
    public String getHostName() {
        if (user != null && user.getName() != null) {
            return user.getName();
        }
        if (user != null && user.getAccount() != null) {
            return user.getAccount().getEmail();
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
