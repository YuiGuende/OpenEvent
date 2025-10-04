package com.group02.openevent.model.user;

import com.group02.openevent.model.event.Event;
import com.group02.openevent.model.organization.Organization;
import jakarta.persistence.*;

import java.util.List;

@Entity
@Table(name = "hosts")
public class Host {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "host_id")
    private Long id;
    private String hostName;
    private String description;
    @OneToOne
    @JoinColumn(name = "customer_id")
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

    public String getHostName() {
        return hostName;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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

    @Override
    public String toString() {
        return "Host{" +
                "id=" + id +
                ", hostName='" + hostName + '\'' +
                ", description='" + description + '\'' +
                ", customer=" + customer +
                ", events=" + events +
                '}';
    }
}
