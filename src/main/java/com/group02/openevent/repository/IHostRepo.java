package com.group02.openevent.repository;

import com.group02.openevent.model.user.Host;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IHostRepo extends JpaRepository<Host, Long> {

    Host getHostById(Long id);

    /**
     * Returns first host for a user (for backward compatibility).
     * Note: A user can have multiple host records, so this returns only the first one.
     * Use findAllByUser_UserId() if you need all hosts.
     * 
     * This method uses native query with LIMIT 1 to avoid NonUniqueResultException
     * when a user has multiple host records.
     */
    @Query(value = "SELECT * FROM hosts WHERE user_id = :userId ORDER BY id ASC LIMIT 1", nativeQuery = true)
    Optional<Host> findByUser_UserId(@Param("userId") Long userId);
    
    List<Host> findAllByUser_UserId(Long userId); // Returns all hosts for a user

}
