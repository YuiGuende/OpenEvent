package com.group02.openevent.model.user;

import com.group02.openevent.model.event.Event;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "event_guests")
public class Guest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false,
        foreignKey = @ForeignKey(name = "fk_eventguest_user"))
    private User user;

    @ManyToOne(optional = false)
    @JoinColumn(name = "event_id", nullable = false,
        foreignKey = @ForeignKey(name = "fk_eventguest_event"))
    private Event event;

    @Column(name = "joined_at", nullable = false)
    private LocalDateTime joinedAt = LocalDateTime.now();

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private GuestStatus status = GuestStatus.ACTIVE;

    // Getters, Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Event getEvent() {
        return event;
    }

    public void setEvent(Event event) {
        this.event = event;
    }

    public LocalDateTime getJoinedAt() {
        return joinedAt;
    }

    public void setJoinedAt(LocalDateTime joinedAt) {
        this.joinedAt = joinedAt;
    }

    public GuestStatus getStatus() {
        return status;
    }

    public void setStatus(GuestStatus status) {
        this.status = status;
    }

    // Enum cho trạng thái Guest
    public enum GuestStatus {
        ACTIVE,   // Đang tham gia
        LEFT,     // Đã rời khỏi event
        REMOVED   // Bị loại khỏi event
    }
}
