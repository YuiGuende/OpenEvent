package com.group02.openevent.model.user;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.group02.openevent.model.account.Account;
import com.group02.openevent.model.admin.Admin;
import com.group02.openevent.model.department.Department;
import com.group02.openevent.model.enums.Role;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "user")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;
    
    @OneToOne(optional = false)
    @JoinColumn(name = "account_id", nullable = false, unique = true,
            foreignKey = @ForeignKey(name = "fk_user_account"))
    @JsonIgnoreProperties({"passwordHash"})
    private Account account;
    
    @Column(name = "name", length = 100)
    private String name;
    
    @Column(name = "phone_number", length = 20)
    private String phoneNumber;
    
    @Column(name = "avatar", length = 500)
    private String avatar;
    
    @Column(name = "date_of_birth")
    private LocalDateTime dateOfBirth;
    
    @Column(name = "address", length = 500)
    private String address;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // Role entities - optional relationships
    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Customer customer;
    
    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Host host;
    
    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Admin admin;
    
    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Department department;
    
    public User() {
    }
    
    public User(Account account) {
        this.account = account;
    }
    
    // Helper methods to determine roles
    public boolean hasCustomerRole() {
        return customer != null;
    }
    
    public boolean hasHostRole() {
        return host != null;
    }
    
    public boolean hasAdminRole() {
        return admin != null;
    }
    
    public boolean hasDepartmentRole() {
        return department != null;
    }
    
    /**
     * Xác định role của User từ các role entities
     * Priority: ADMIN > DEPARTMENT > HOST > CUSTOMER
     */
    public Role getRole() {
        if (admin != null) {
            return Role.ADMIN;
        }
        if (department != null) {
            return Role.DEPARTMENT;
        }
        if (host != null) {
            return Role.HOST;
        }
        if (customer != null) {
            return Role.CUSTOMER;
        }
        // Default fallback
        return Role.CUSTOMER;
    }
    
    @Override
    public String toString() {
        return "User{" +
                "userId=" + userId +
                ", name='" + name + '\'' +
                ", phoneNumber='" + phoneNumber + '\'' +
                ", email='" + (account != null ? account.getEmail() : null) + '\'' +
                '}';
    }
}

