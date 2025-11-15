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

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "points", nullable = false)
    private Integer points = 0;

    @Column(unique = true, length = 100)
    private String memberID;

    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;  // URL to customer's profile photo for face recognition

    @Column(name = "face_registered", nullable = false)
    private Boolean faceRegistered = false;  // Whether customer has registered face for check-in

    public Customer() {
    }

} 