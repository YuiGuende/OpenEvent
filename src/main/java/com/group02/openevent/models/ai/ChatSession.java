package com.group02.openevent.models.ai;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entity for chat sessions
 */
@Entity
@Table(name = "chat_session")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatSession {
    
    @Id
    @Column(name = "session_id")
    private String sessionId;
    
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    @Column(name = "title", nullable = false, length = 255)
    private String title;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}