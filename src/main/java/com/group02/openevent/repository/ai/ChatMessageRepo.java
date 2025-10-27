package com.group02.openevent.repository.ai;

import com.group02.openevent.models.ai.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for ChatMessage entity
 */
@Repository
public interface ChatMessageRepo extends JpaRepository<ChatMessage, Long> {
    
    /**
     * Find messages by user ID and session ID ordered by timestamp ascending
     */
    List<ChatMessage> findByUserIdAndSessionIdOrderByTimestampAsc(Long userId, String sessionId);
    
    /**
     * Find messages by user ID ordered by timestamp ascending
     */
    List<ChatMessage> findByUserIdOrderByTimestampAsc(Long userId);
    
    /**
     * Find distinct session IDs by user ID
     */
    @Query("SELECT DISTINCT cm.sessionId FROM ChatMessage cm WHERE cm.userId = :userId")
    List<Long> findDistinctSessionIdsByUserId(Long userId);
    
    /**
     * Count messages by user ID and session ID
     */
    Long countByUserIdAndSessionId(Long userId, String sessionId);
    
    /**
     * Delete messages by user ID and session ID
     */
    void deleteByUserIdAndSessionId(Long userId, String sessionId);
    
    /**
     * Delete messages by user ID
     */
    void deleteByUserId(Long userId);
}