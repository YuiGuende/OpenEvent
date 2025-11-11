package com.group02.openevent.repository;

import com.group02.openevent.model.account.Account;
import com.group02.openevent.model.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IUserRepo extends JpaRepository<User, Long> {
    Optional<User> findByAccount(Account account);
    Optional<User> findByAccount_AccountId(Long accountId);
    Optional<User> findByAccount_Email(String email);

    User findByHost_Id(Long hostId);

    boolean existsByPhoneNumber(String phoneNumber);
}
