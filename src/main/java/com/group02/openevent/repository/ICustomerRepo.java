package com.group02.openevent.repository;

import com.group02.openevent.model.user.Customer;
import com.group02.openevent.model.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ICustomerRepo extends JpaRepository<Customer, Long> {
    Optional<Customer> findByUser(User user);
    Optional<Customer> findByUser_UserId(Long userId);
    Optional<Customer> findByUser_Account_AccountId(Long accountId);
    Optional<Customer> findByUser_Account_Email(String email);
} 