package com.group02.openevent.service.impl;

import com.group02.openevent.model.account.Account;
import com.group02.openevent.model.session.Session;
import com.group02.openevent.repository.ISessionRepo;
import com.group02.openevent.service.SessionService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class SessionServiceImpl implements SessionService {
    
    private final ISessionRepo sessionRepo;
    
    @Value("${session.max-concurrent:5}")
    private int maxConcurrentSessions;
    
    @Value("${session.timeout-hours:24}")
    private int sessionTimeoutHours;
    
    public SessionServiceImpl(ISessionRepo sessionRepo) {
        this.sessionRepo = sessionRepo;
    }
    
    @Override
    public Session createSession(Account account, HttpServletRequest request) {
        if (!canCreateNewSession(account.getAccountId())) {

            List<Session> activeSessions = sessionRepo.findActiveSessionsByAccountId(
                account.getAccountId(), LocalDateTime.now());
            if (!activeSessions.isEmpty()) {
                Session oldestSession = activeSessions.stream()
                    .min((s1, s2) -> s1.getLastAccessedAt().compareTo(s2.getLastAccessedAt()))
                    .orElse(null);
                if (oldestSession != null) {
                    sessionRepo.deactivateSessionByToken(oldestSession.getSessionToken());
                }
            }
        }

        String sessionToken = generateSessionToken();

        String ipAddress = getClientIpAddress(request);
        String userAgent = request.getHeader("User-Agent");
        String deviceInfo = extractDeviceInfo(userAgent);

        Session session = new Session(sessionToken, account, ipAddress, userAgent);
        session.setDeviceInfo(deviceInfo);
        session.setExpiresAt(LocalDateTime.now().plusHours(sessionTimeoutHours));
        
        return sessionRepo.save(session);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Optional<Session> validateSession(String sessionToken) {
        if (sessionToken == null || sessionToken.trim().isEmpty()) {
            return Optional.empty();
        }
        
        // Handle HTTP session fallback
        if (sessionToken.startsWith("HTTP_SESSION_")) {
            // This is a fallback for HTTP session, return empty
            return Optional.empty();
        }
        
        Optional<Session> sessionOpt = sessionRepo.findActiveSessionByToken(sessionToken, LocalDateTime.now());
        
        if (sessionOpt.isPresent()) {
            Session session = sessionOpt.get();
            // Update last accessed time
            session.updateLastAccessed();
            sessionRepo.updateLastAccessedTime(sessionToken, session.getLastAccessedAt());
        }
        
        return sessionOpt;
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<Session> getActiveSessions(Long accountId) {
        return sessionRepo.findActiveSessionsByAccountId(accountId, LocalDateTime.now());
    }
    
    @Override
    public boolean destroySession(String sessionToken) {
        try {
            sessionRepo.deactivateSessionByToken(sessionToken);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    @Override
    public void destroyAllUserSessions(Long accountId) {
        sessionRepo.deactivateAllSessionsByAccountId(accountId);
    }
    
    @Override
    public void updateSessionActivity(String sessionToken) {
        sessionRepo.updateLastAccessedTime(sessionToken, LocalDateTime.now());
    }
    
    @Override
    public boolean extendSession(String sessionToken, int hours) {
        Optional<Session> sessionOpt = sessionRepo.findBySessionToken(sessionToken);
        if (sessionOpt.isPresent() && sessionOpt.get().getIsActive()) {
            Session session = sessionOpt.get();
            session.extendSession(hours);
            sessionRepo.save(session);
            return true;
        }
        return false;
    }
    
    @Override
    @Transactional(readOnly = true)
    public boolean canCreateNewSession(Long accountId) {
        long currentSessionCount = sessionRepo.countActiveSessionsByAccountId(accountId, LocalDateTime.now());
        return currentSessionCount < maxConcurrentSessions;
    }
    
    @Override
    public void cleanupExpiredSessions() {
        sessionRepo.deleteExpiredSessions(LocalDateTime.now());
    }
    
    @Override
    @Transactional(readOnly = true)
    public long getSessionCount(Long accountId) {
        return sessionRepo.countActiveSessionsByAccountId(accountId, LocalDateTime.now());
    }
    
    @Override
    @Transactional(readOnly = true)
    public boolean isSessionValid(String sessionToken) {
        return validateSession(sessionToken).isPresent();
    }
    
    // Helper methods
    private String generateSessionToken() {
        return UUID.randomUUID().toString().replace("-", "") + 
               System.currentTimeMillis();
    }
    
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
    
    private String extractDeviceInfo(String userAgent) {
        if (userAgent == null) return "Unknown";
        
        if (userAgent.toLowerCase().contains("mobile")) {
            return "Mobile";
        } else if (userAgent.toLowerCase().contains("tablet")) {
            return "Tablet";
        } else if (userAgent.toLowerCase().contains("windows")) {
            return "Windows";
        } else if (userAgent.toLowerCase().contains("mac")) {
            return "Mac";
        } else if (userAgent.toLowerCase().contains("linux")) {
            return "Linux";
        } else {
            return "Desktop";
        }
    }
}
