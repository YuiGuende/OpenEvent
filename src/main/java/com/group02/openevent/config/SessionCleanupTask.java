package com.group02.openevent.config;

import com.group02.openevent.service.SessionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class SessionCleanupTask {
    
    private static final Logger logger = LoggerFactory.getLogger(SessionCleanupTask.class);
    
    @Autowired
    private SessionService sessionService;
    
    @Value("${session.auto-cleanup:true}")
    private boolean autoCleanupEnabled;
    
    @Value("${session.cleanup-interval:3600}")
    private long cleanupIntervalSeconds;
    
    /**
     * Clean up expired sessions every hour (3600 seconds)
     */
    @Scheduled(fixedRateString = "${session.cleanup-interval:3600}000")
    public void cleanupExpiredSessions() {
        if (!autoCleanupEnabled) {
            logger.debug("Session auto-cleanup is disabled");
            return;
        }
        
        try {
            logger.info("Starting session cleanup task...");
            long startTime = System.currentTimeMillis();
            
            sessionService.cleanupExpiredSessions();
            
            long duration = System.currentTimeMillis() - startTime;
            logger.info("Session cleanup completed in {} ms", duration);
            
        } catch (Exception e) {
            logger.error("Error during session cleanup: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Manual cleanup method that can be called programmatically
     */
    public void performCleanup() {
        logger.info("Performing manual session cleanup...");
        try {
            sessionService.cleanupExpiredSessions();
            logger.info("Manual session cleanup completed successfully");
        } catch (Exception e) {
            logger.error("Error during manual session cleanup: {}", e.getMessage(), e);
        }
    }
}
