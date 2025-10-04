package com.group02.openevent.repository;

import com.group02.openevent.model.session.Session;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ISessionRepo extends JpaRepository<Session, Long> {
    
    /**
     * Find active session by token
     */
    @Query("SELECT s FROM Session s WHERE s.sessionToken = :token AND s.isActive = true AND s.expiresAt > :now")
    Optional<Session> findActiveSessionByToken(@Param("token") String token, @Param("now") LocalDateTime now);
    
    /**
     * Find all active sessions for a user
     */
    @Query("SELECT s FROM Session s WHERE s.account.accountId = :accountId AND s.isActive = true AND s.expiresAt > :now")
    List<Session> findActiveSessionsByAccountId(@Param("accountId") Long accountId, @Param("now") LocalDateTime now);
    
    /**
     * Find session by token (including expired ones)
     */
    Optional<Session> findBySessionToken(String sessionToken);
    
    /**
     * Deactivate all sessions for a user
     */
    @Modifying
    @Query("UPDATE Session s SET s.isActive = false WHERE s.account.accountId = :accountId")
    void deactivateAllSessionsByAccountId(@Param("accountId") Long accountId);
    
    /**
     * Deactivate specific session
     */
    @Modifying
    @Query("UPDATE Session s SET s.isActive = false WHERE s.sessionToken = :token")
    void deactivateSessionByToken(@Param("token") String token);
    
    /**
     * Find expired sessions
     */
    @Query("SELECT s FROM Session s WHERE s.expiresAt <= :now AND s.isActive = true")
    List<Session> findExpiredSessions(@Param("now") LocalDateTime now);
    
    /**
     * Delete expired sessions
     */
    @Modifying
    @Query("DELETE FROM Session s WHERE s.expiresAt <= :now")
    void deleteExpiredSessions(@Param("now") LocalDateTime now);
    
    /**
     * Count active sessions for a user
     */
    @Query("SELECT COUNT(s) FROM Session s WHERE s.account.accountId = :accountId AND s.isActive = true AND s.expiresAt > :now")
    long countActiveSessionsByAccountId(@Param("accountId") Long accountId, @Param("now") LocalDateTime now);
    
    /**
     * Find sessions by IP address
     */
    @Query("SELECT s FROM Session s WHERE s.ipAddress = :ipAddress AND s.isActive = true AND s.expiresAt > :now")
    List<Session> findActiveSessionsByIpAddress(@Param("ipAddress") String ipAddress, @Param("now") LocalDateTime now);
    
    /**
     * Update last accessed time
     */
    @Modifying
    @Query("UPDATE Session s SET s.lastAccessedAt = :lastAccessed WHERE s.sessionToken = :token")
    void updateLastAccessedTime(@Param("token") String token, @Param("lastAccessed") LocalDateTime lastAccessed);
}
