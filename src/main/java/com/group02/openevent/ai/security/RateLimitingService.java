package com.group02.openevent.ai.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Service for implementing rate limiting for AI endpoints
 */
@Service
@Slf4j
public class RateLimitingService {

    // Rate limit configurations
    private static final int DEFAULT_REQUESTS_PER_MINUTE = 30;
    private static final int DEFAULT_REQUESTS_PER_HOUR = 500;
    private static final int DEFAULT_REQUESTS_PER_DAY = 2000;
    
    // AI-specific rate limits (more restrictive)
    private static final int AI_REQUESTS_PER_MINUTE = 20;
    private static final int AI_REQUESTS_PER_HOUR = 300;
    private static final int AI_REQUESTS_PER_DAY = 1000;
    
    // Translation rate limits
    private static final int TRANSLATION_REQUESTS_PER_MINUTE = 50;
    private static final int TRANSLATION_REQUESTS_PER_HOUR = 1000;
    
    // Rate limit tracking
    private final ConcurrentHashMap<String, RateLimitTracker> rateLimitMap = new ConcurrentHashMap<>();
    
    // Cleanup task to remove expired entries
    private final ConcurrentHashMap<String, LocalDateTime> lastCleanup = new ConcurrentHashMap<>();

    /**
     * Check if request is allowed for user
     */
    public boolean isAllowed(String userId, RateLimitType type) {
        String key = generateKey(userId, type);
        RateLimitTracker tracker = rateLimitMap.computeIfAbsent(key, k -> new RateLimitTracker(type));
        
        // Cleanup old entries periodically
        cleanupExpiredEntries();
        
        return tracker.isAllowed();
    }

    /**
     * Get remaining requests for user
     */
    public RateLimitInfo getRateLimitInfo(String userId, RateLimitType type) {
        String key = generateKey(userId, type);
        RateLimitTracker tracker = rateLimitMap.get(key);
        
        if (tracker == null) {
            return new RateLimitInfo(getMaxRequests(type), getMaxRequests(type), LocalDateTime.now().plusMinutes(1));
        }
        
        return tracker.getRateLimitInfo();
    }

    /**
     * Reset rate limit for user (admin function)
     */
    public void resetRateLimit(String userId, RateLimitType type) {
        String key = generateKey(userId, type);
        rateLimitMap.remove(key);
        log.info("Rate limit reset for user: {} type: {}", userId, type);
    }

    /**
     * Get rate limit status for all types
     */
    public Map<RateLimitType, RateLimitInfo> getAllRateLimitInfo(String userId) {
        Map<RateLimitType, RateLimitInfo> info = new HashMap<>();
        
        for (RateLimitType type : RateLimitType.values()) {
            info.put(type, getRateLimitInfo(userId, type));
        }
        
        return info;
    }

    private String generateKey(String userId, RateLimitType type) {
        return userId + ":" + type.name();
    }

    private int getMaxRequests(RateLimitType type) {
        return switch (type) {
            case AI_CHAT -> AI_REQUESTS_PER_MINUTE;
            case AI_TRANSLATION -> TRANSLATION_REQUESTS_PER_MINUTE;
            case GENERAL -> DEFAULT_REQUESTS_PER_MINUTE;
        };
    }

    private void cleanupExpiredEntries() {
        LocalDateTime now = LocalDateTime.now();
        String currentMinute = now.toString().substring(0, 16); // YYYY-MM-DDTHH:MM
        
        if (!lastCleanup.containsKey(currentMinute)) {
            lastCleanup.put(currentMinute, now);
            
            // Remove entries older than 1 hour
            rateLimitMap.entrySet().removeIf(entry -> {
                RateLimitTracker tracker = entry.getValue();
                return tracker.isExpired(now.minusHours(1));
            });
            
            log.debug("Cleaned up expired rate limit entries. Current size: {}", rateLimitMap.size());
        }
    }

    /**
     * Rate limit types
     */
    public enum RateLimitType {
        AI_CHAT,
        AI_TRANSLATION,
        GENERAL
    }

    /**
     * Rate limit tracker for individual user
     */
    private static class RateLimitTracker {
        private final AtomicInteger requestCount = new AtomicInteger(0);
        private final LocalDateTime windowStart;
        private final RateLimitType type;
        private final int maxRequests;

        public RateLimitTracker(RateLimitType type) {
            this.type = type;
            this.windowStart = LocalDateTime.now();
            this.maxRequests = getMaxRequestsForType(type);
        }

        public boolean isAllowed() {
            LocalDateTime now = LocalDateTime.now();
            
            // Reset counter if window has passed
            if (now.isAfter(windowStart.plusMinutes(1))) {
                requestCount.set(0);
                return true;
            }
            
            int current = requestCount.incrementAndGet();
            boolean allowed = current <= maxRequests;
            
            if (!allowed) {
                log.warn("Rate limit exceeded for type: {} current: {} max: {}", type, current, maxRequests);
            }
            
            return allowed;
        }

        public RateLimitInfo getRateLimitInfo() {
            LocalDateTime now = LocalDateTime.now();
            int current = requestCount.get();
            int remaining = Math.max(0, maxRequests - current);
            LocalDateTime resetTime = windowStart.plusMinutes(1);
            
            return new RateLimitInfo(maxRequests, remaining, resetTime);
        }

        public boolean isExpired(LocalDateTime cutoff) {
            return windowStart.isBefore(cutoff);
        }

        private int getMaxRequestsForType(RateLimitType type) {
            return switch (type) {
                case AI_CHAT -> AI_REQUESTS_PER_MINUTE;
                case AI_TRANSLATION -> TRANSLATION_REQUESTS_PER_MINUTE;
                case GENERAL -> DEFAULT_REQUESTS_PER_MINUTE;
            };
        }
    }

    /**
     * Rate limit information
     */
    public static class RateLimitInfo {
        private final int maxRequests;
        private final int remainingRequests;
        private final LocalDateTime resetTime;

        public RateLimitInfo(int maxRequests, int remainingRequests, LocalDateTime resetTime) {
            this.maxRequests = maxRequests;
            this.remainingRequests = remainingRequests;
            this.resetTime = resetTime;
        }

        public int getMaxRequests() {
            return maxRequests;
        }

        public int getRemainingRequests() {
            return remainingRequests;
        }

        public LocalDateTime getResetTime() {
            return resetTime;
        }

        public boolean isLimitExceeded() {
            return remainingRequests <= 0;
        }
    }
}
