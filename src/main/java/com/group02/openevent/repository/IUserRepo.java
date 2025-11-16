package com.group02.openevent.repository;

import com.group02.openevent.model.account.Account;
import com.group02.openevent.model.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface IUserRepo extends JpaRepository<User, Long> {
    Optional<User> findByAccount(Account account);
    Optional<User> findByAccount_AccountId(Long accountId);
    Optional<User> findByAccount_Email(String email);

    @Query("SELECT u FROM User u JOIN u.hosts h WHERE h.id = :hostId")
    User findByHost_Id(@Param("hostId") Long hostId);
    
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.hosts LEFT JOIN FETCH u.customer LEFT JOIN FETCH u.admin LEFT JOIN FETCH u.department WHERE u.account.accountId = :accountId")
    Optional<User> findByAccountIdWithRoles(@Param("accountId") Long accountId);

    boolean existsByPhoneNumber(String phoneNumber);
}
