package com.group02.openevent.repository;

import com.group02.openevent.model.user.Host;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface IHostRepo extends JpaRepository<Host, Long> {
    Optional<Host> findByCustomer_CustomerId(Long customerId);
    Host getHostById(Long id);
    @Query("SELECT h FROM Host h JOIN h.customer c JOIN c.account a WHERE a.accountId = :accountId")
    Optional<Host> findByAccountId(@Param("accountId") Long accountId);
}
