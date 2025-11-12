package com.group02.openevent.model.chat;

import com.group02.openevent.model.event.Event;
import com.group02.openevent.model.user.User;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(
        name = "event_chat_room",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_room_event_host_volunteer",
                columnNames = {"event_id", "host_user_id", "volunteer_user_id"}
        )
)
public class EventChatRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_chat_room_event"))
    private Event event;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "host_user_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_chat_room_host_user"))
    private User host;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "volunteer_user_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_chat_room_volunteer_user"))
    private User volunteer;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}


