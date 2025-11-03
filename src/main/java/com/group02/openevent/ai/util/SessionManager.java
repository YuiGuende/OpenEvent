package com.group02.openevent.ai.util;

import com.group02.openevent.ai.service.EventAIAgent;
import com.group02.openevent.ai.exception.AIException;
import com.group02.openevent.ai.exception.AIErrorCodes;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class SessionManager {

    private static final Map<String, SessionInfo> sessions = new ConcurrentHashMap<>();
    private static EventAIAgent defaultAgent;
    
    // Session timeout: 30 minutes
    private static final long SESSION_TIMEOUT_MINUTES = 30;
    
    // Session info wrapper
    private static class SessionInfo {
        private final EventAIAgent agent;
        private final LocalDateTime createdAt;
        private LocalDateTime lastAccessed;
        
        public SessionInfo(EventAIAgent agent) {
            this.agent = agent;
            this.createdAt = LocalDateTime.now();
            this.lastAccessed = LocalDateTime.now();
        }
        
        public EventAIAgent getAgent() { return agent; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public LocalDateTime getLastAccessed() { return lastAccessed; }
        
        public void updateLastAccessed() {
            this.lastAccessed = LocalDateTime.now();
        }
        
        public boolean isExpired() {
            return lastAccessed.isBefore(LocalDateTime.now().minusMinutes(SESSION_TIMEOUT_MINUTES));
        }
    }

    public static void setDefaultAgent(EventAIAgent agent) {
        defaultAgent = agent;
        log.info("Default AI Agent set for SessionManager");
    }

    public static EventAIAgent get(String sessionId) throws AIException {
        if (sessionId == null || sessionId.trim().isEmpty()) {
            throw new AIException(AIErrorCodes.SESSION_INVALID, "Session ID cannot be null or empty", 
                    "❌ Session không hợp lệ");
        }
        
        SessionInfo sessionInfo = sessions.get(sessionId);
        if (sessionInfo == null) {
            throw new AIException(AIErrorCodes.SESSION_NOT_FOUND, "Session not found: " + sessionId, 
                    "❌ Không tìm thấy session. Vui lòng tạo session mới.");
        }
        
        if (sessionInfo.isExpired()) {
            sessions.remove(sessionId);
            throw new AIException(AIErrorCodes.SESSION_EXPIRED, "Session expired: " + sessionId, 
                    "❌ Session đã hết hạn. Vui lòng tạo session mới.");
        }
        
        sessionInfo.updateLastAccessed();
        return sessionInfo.getAgent();
    }

    public static EventAIAgent getOrCreate(String sessionId) throws AIException {
        try {
            return get(sessionId);
        } catch (AIException e) {
            if (AIErrorCodes.SESSION_NOT_FOUND.equals(e.getErrorCode())) {
                return createSession(sessionId);
            }
            throw e;
        }
    }
    
    private static EventAIAgent createSession(String sessionId) throws AIException {
        if (defaultAgent == null) {
            throw new AIException(AIErrorCodes.SERVICE_UNAVAILABLE, "Default AI Agent not initialized", 
                    "❌ Hệ thống AI chưa sẵn sàng. Vui lòng thử lại sau.");
        }
        
        SessionInfo sessionInfo = new SessionInfo(defaultAgent);
        sessions.put(sessionId, sessionInfo);
        log.info("Created new AI session: {}", sessionId);
        return defaultAgent;
    }
    
    public static void put(String sessionId, EventAIAgent agent) {
        if (sessionId != null && agent != null) {
            sessions.put(sessionId, new SessionInfo(agent));
            log.info("Added AI session: {}", sessionId);
        }
    }
    
    public static void remove(String sessionId) {
        if (sessionId != null) {
            sessions.remove(sessionId);
            log.info("Removed AI session: {}", sessionId);
        }
    }
    
    public static void clearAll() {
        int count = sessions.size();
        sessions.clear();
        log.info("Cleared all AI sessions. Removed {} sessions", count);
    }
    
    public static int getSessionCount() {
        return sessions.size();
    }
    
    /**
     * Cleanup expired sessions every 10 minutes
     */
    @Scheduled(fixedRate = 600000) // 10 minutes
    public static void cleanupExpiredSessions() {
        int removedCount = 0;
        for (Map.Entry<String, SessionInfo> entry : sessions.entrySet()) {
            if (entry.getValue().isExpired()) {
                sessions.remove(entry.getKey());
                removedCount++;
            }
        }
        if (removedCount > 0) {
            log.info("Cleaned up {} expired AI sessions", removedCount);
        }
    }
    
    /**
     * Get session statistics
     */
    public static Map<String, Object> getSessionStats() {
        return Map.of(
            "totalSessions", sessions.size(),
            "oldestSession", sessions.values().stream()
                .map(SessionInfo::getCreatedAt)
                .min(LocalDateTime::compareTo)
                .orElse(null),
            "newestSession", sessions.values().stream()
                .map(SessionInfo::getCreatedAt)
                .max(LocalDateTime::compareTo)
                .orElse(null)
        );
    }
}
