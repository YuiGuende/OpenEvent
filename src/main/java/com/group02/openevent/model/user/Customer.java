package com.group02.openevent.model.user;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.group02.openevent.model.account.Account;
import com.group02.openevent.model.organization.Organization;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "customer")
public class Customer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "customer_id")
    private Long customerId;

    @OneToOne(optional = false)
    @JoinColumn(name = "account_id", nullable = false, unique = true,
            foreignKey = @ForeignKey(name = "fk_user_account"))
    @JsonIgnoreProperties({"passwordHash"})
    private Account account;

    @Column(name = "email", length = 100)
    private String email;

    private String name;

    @ManyToOne
    @JoinColumn(name = "organization_id", 
            foreignKey = @ForeignKey(name = "fk_user_org"))
    private Organization organization;

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    @Column(name = "points", nullable = false)
    private Integer points = 0;

    @OneToOne(mappedBy = "customer", cascade = CascadeType.ALL)
    private Host host;

    public Customer() {
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public Host getHost() {
        return host;
    }

    public void setHost(Host host) {
        this.host = host;
    }

    public Account getAccount() {
        return account;
    }

    public void setAccount(Account account) {
        this.account = account;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public Integer getPoints() {
        return points;
    }

    public void setPoints(Integer points) {
        this.points = points;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Organization getOrganization() {
        return organization;
    }

    public void setOrganization(Organization organization) {
        this.organization = organization;
    }
} 