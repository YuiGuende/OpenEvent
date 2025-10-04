package com.group02.openevent.repository;

import com.group02.openevent.model.ai.ChatHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository interface cho ChatHistory
 * @author Admin
 */
@Repository
public interface IChatHistoryRepo extends JpaRepository<ChatHistory, Integer> {
    
    /**
     * Tìm tất cả chat history của một user theo thứ tự timestamp
     */
    List<ChatHistory> findByUserIdOrderByTimestampAsc(Integer userId);
    
    /**
     * Tìm chat history của một user và session cụ thể theo thứ tự timestamp
     */
    List<ChatHistory> findByUserIdAndSessionIdOrderByTimestampAsc(Integer userId, String sessionId);
    
    /**
     * Tìm tất cả session IDs của một user
     */
    @Query("SELECT DISTINCT ch.sessionId FROM ChatHistory ch WHERE ch.userId = :userId")
    List<String> findDistinctSessionIdsByUserId(Integer userId);
    
    /**
     * Xóa chat history của một user và session
     */
    void deleteByUserIdAndSessionId(Integer userId, String sessionId);
    
    /**
     * Đếm số lượng chat history của một user và session
     */
    long countByUserIdAndSessionId(Integer userId, String sessionId);
    
    /**
     * Tìm chat history gần nhất của một user
     */
    @Query("SELECT ch FROM ChatHistory ch WHERE ch.userId = :userId ORDER BY ch.timestamp DESC")
    List<ChatHistory> findLatestByUserId(Integer userId);
    
    /**
     * Tìm chat history của một session theo thứ tự timestamp
     */
    List<ChatHistory> findBySessionIdOrderByTimestampAsc(String sessionId);
}
