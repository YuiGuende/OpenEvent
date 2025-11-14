package com.group02.openevent.model.chat;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.group02.openevent.model.user.User;
import jakarta.persistence.*;
import lombok.Data;

import java.time.Instant;

@Data
@Entity
@Table(name = "event_chat_message")
public class EventChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_chat_message_room"))
    @JsonIgnore
    private EventChatRoom chatRoom;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_user_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_chat_message_sender_user"))
    private User sender;

    @Column(name = "body", nullable = false, columnDefinition = "TEXT")
    private String body;

    @Column(name = "timestamp", nullable = false)
    private Instant timestamp;

    @Column(name = "is_read", nullable = false)
    private boolean isRead = false;

    @PrePersist
    protected void onCreate() {
        if (timestamp == null) {
            timestamp = Instant.now();
        }
    }
}


