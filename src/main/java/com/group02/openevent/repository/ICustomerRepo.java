package com.group02.openevent.repository;

import com.group02.openevent.model.account.Account;
import com.group02.openevent.model.user.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ICustomerRepo extends JpaRepository<Customer, Long> {
    boolean existsByPhoneNumber(String phoneNumber);
    Optional<Customer> findByAccount(Account account);
    Optional<Customer> findByAccount_AccountId(Long accountId);
} 