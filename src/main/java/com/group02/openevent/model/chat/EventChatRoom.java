package com.group02.openevent.model.chat;

import com.group02.openevent.model.event.Event;
import com.group02.openevent.model.user.User;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@Table(
        name = "event_chat_room",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_room_host_department",
                        columnNames = {"host_user_id", "department_user_id", "room_type"}
                ),
                @UniqueConstraint(
                        name = "uk_room_host_event_volunteers",
                        columnNames = {"host_user_id", "event_id", "room_type"}
                )
        }
)
public class EventChatRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "host_user_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_chat_room_host_user"))
    private User host;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_user_id", nullable = true,
            foreignKey = @ForeignKey(name = "fk_chat_room_department_user"))
    private User department;  // For HOST_DEPARTMENT room type

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = true,
            foreignKey = @ForeignKey(name = "fk_chat_room_event"))
    private Event event;  // For HOST_VOLUNTEERS room type (nullable for HOST_DEPARTMENT)

    @Enumerated(EnumType.STRING)
    @Column(name = "room_type", nullable = false, length = 20)
    private ChatRoomType roomType;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    // Participants for HOST_VOLUNTEERS room type
    @OneToMany(mappedBy = "room", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<EventChatRoomParticipant> participants = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}


