package com.group02.openevent.model.volunteer;

import com.group02.openevent.model.event.Event;
import com.group02.openevent.model.user.Customer;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "volunteer_application")
public class VolunteerApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "volunteer_application_id")
    private Long volunteerApplicationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_volunteer_application_customer"))
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_volunteer_application_event"))
    private Event event;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private VolunteerStatus status = VolunteerStatus.PENDING;

    @Column(name = "application_message", columnDefinition = "TEXT")
    private String applicationMessage; // Lời nhắn từ volunteer (optional)

    @Column(name = "host_response", columnDefinition = "TEXT")
    private String hostResponse; // Phản hồi từ host (optional)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by_account_id", nullable = true,
            foreignKey = @ForeignKey(name = "fk_volunteer_application_reviewer"))
    private com.group02.openevent.model.account.Account reviewedBy; // Host đã review

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt; // Thời điểm host review

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Constructors
    public VolunteerApplication() {
    }

    public VolunteerApplication(Customer customer, Event event) {
        this.customer = customer;
        this.event = event;
        this.status = VolunteerStatus.PENDING;
    }

    // Getters and Setters
    public Long getVolunteerApplicationId() {
        return volunteerApplicationId;
    }

    public void setVolunteerApplicationId(Long volunteerApplicationId) {
        this.volunteerApplicationId = volunteerApplicationId;
    }

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }

    public Event getEvent() {
        return event;
    }

    public void setEvent(Event event) {
        this.event = event;
    }

    public VolunteerStatus getStatus() {
        return status;
    }

    public void setStatus(VolunteerStatus status) {
        this.status = status;
    }

    public String getApplicationMessage() {
        return applicationMessage;
    }

    public void setApplicationMessage(String applicationMessage) {
        this.applicationMessage = applicationMessage;
    }

    public String getHostResponse() {
        return hostResponse;
    }

    public void setHostResponse(String hostResponse) {
        this.hostResponse = hostResponse;
    }

    public com.group02.openevent.model.account.Account getReviewedBy() {
        return reviewedBy;
    }

    public void setReviewedBy(com.group02.openevent.model.account.Account reviewedBy) {
        this.reviewedBy = reviewedBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public LocalDateTime getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(LocalDateTime reviewedAt) {
        this.reviewedAt = reviewedAt;
    }
}

