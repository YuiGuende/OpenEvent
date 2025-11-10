package com.group02.openevent.model.user;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.group02.openevent.model.organization.Organization;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "customer")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Customer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "customer_id")
    private Long customerId;

    @OneToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true,
            foreignKey = @ForeignKey(name = "fk_customer_user"))
    @JsonIgnoreProperties({"account", "customer", "host", "admin", "department"})
    private User user;

    @ManyToOne
    @JoinColumn(name = "organization_id", 
            foreignKey = @ForeignKey(name = "fk_user_org"))
    private Organization organization;

    @Column(name = "points", nullable = false)
    private Integer points = 0;

//    @OneToOne(mappedBy = "customer", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
//    private Host host;

    @Column(unique = true, length = 100)
    private String memberID;

    public Customer() {
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }


    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Integer getPoints() {
        return points;
    }

    public void setPoints(Integer points) {
        this.points = points;
    }

    public Organization getOrganization() {
        return organization;
    }

    public void setOrganization(Organization organization) {
        this.organization = organization;
    }
} 