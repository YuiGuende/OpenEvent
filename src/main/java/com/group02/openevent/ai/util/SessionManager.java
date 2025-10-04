package com.group02.openevent.ai.util;

import com.group02.openevent.ai.service.EventAIAgent;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SessionManager {

    private static final Map<String, EventAIAgent> sessions = new ConcurrentHashMap<>();
    private static EventAIAgent defaultAgent;

    public static void setDefaultAgent(EventAIAgent agent) {
        defaultAgent = agent;
    }

    public static EventAIAgent get(String sessionId) {
        return sessions.computeIfAbsent(sessionId, k -> {
            if (defaultAgent != null) {
                // For now, we'll use the same instance for all sessions
                // In a production system, you might want to create new instances
                return defaultAgent;
            }
            throw new IllegalStateException("Default EventAIAgent not set. Please ensure SessionManager is properly initialized.");
        });
    }

    public static void put(String sessionId, EventAIAgent agent) {
        sessions.put(sessionId, agent);
    }
    
    public static EventAIAgent getOrCreate(String sessionId) {
        return get(sessionId);
    }
    
    public static void remove(String sessionId) {
        sessions.remove(sessionId);
    }
    
    public static void clearAll() {
        sessions.clear();
    }
    
    public static int getSessionCount() {
        return sessions.size();
    }
}
