package com.group02.openevent.service;

import com.group02.openevent.model.account.Account;
import com.group02.openevent.model.user.Customer;
import com.group02.openevent.model.user.User;
import jakarta.servlet.http.HttpSession;

import java.util.List;
import java.util.Optional;

public interface UserService {
    /**
     * Get or create User for an Account
     */
    User getOrCreateUser(Account account);
    
    /**
     * Get User by Account ID
     */
    Optional<User> getUserByAccountId(Long accountId);
    
    /**
     * Get Customer by Account ID (for backward compatibility)
     */
    Optional<Customer> getCustomerByAccountId(Long accountId);
    
    /**
     * Create User from Account with basic info
     */
    User createUserFromAccount(Account account);

    User getCurrentUser(HttpSession session);

    User getUserById(Long id);

    User getUserByHostId(Long hostId);

    List<User> findAllById(List<Long> receiverAccountIds);
}