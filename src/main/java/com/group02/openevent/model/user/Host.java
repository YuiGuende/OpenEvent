package com.group02.openevent.model.user;

import com.group02.openevent.model.event.Event;
import com.group02.openevent.model.organization.Organization;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "host")
public class Host {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "host_id")
    private Long id;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @ManyToOne
    @JoinColumn(name = "organize_id", 
            foreignKey = @ForeignKey(name = "fk_host_org"))
    private Organization organization;
    
    @ManyToOne(optional = false)
    @JoinColumn(name = "event_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_host_event"))
    private Event event;
    
    @OneToOne(optional = false)
    @JoinColumn(name = "customer_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_host_customer"))
    private Customer customer;



    // Host có thể trực tiếp tạo nhiều sự kiện
    @OneToMany(mappedBy = "host", cascade = CascadeType.ALL)
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

    public Event getEvent() {
        return event;
    }

    public void setEvent(Event event) {
        this.event = event;
    }

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
                ", organization=" + organization +
                ", event=" + event +
                ", customer=" + customer +
                '}';
    }
}
