package com.group02.openevent.repository.ai;

import com.group02.openevent.models.ai.ChatSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for ChatSession entity
 */
@Repository
public interface ChatSessionRepo extends JpaRepository<ChatSession, Long> {
    
    /**
     * Find sessions by user ID ordered by creation date descending
     */
    List<ChatSession> findByUserIdOrderByCreatedAtDesc(Long userId);
    
    /**
     * Find session by user ID and session ID
     */
    @Query("SELECT cs FROM ChatSession cs WHERE cs.userId = :userId AND cs.sessionId = :sessionId")
    ChatSession findByUserIdAndSessionId(Long userId, String sessionId);
    
    /**
     * Delete sessions by user ID
     */
    void deleteByUserId(Long userId);
}