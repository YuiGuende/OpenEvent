package com.group02.openevent.service;

import com.group02.openevent.model.account.Account;
import com.group02.openevent.model.user.Customer;
import com.group02.openevent.model.user.User;
import jakarta.servlet.http.HttpSession;

import java.util.List;
import java.util.Optional;

public interface UserService {
    User getOrCreateUser(Account account);
    Optional<User> getUserByAccountId(Long accountId);
    Optional<Customer> getCustomerByAccountId(Long accountId);
    User createUserFromAccount(Account account);

    User getCurrentUser(HttpSession session);

    User getUserById(Long id);

    User getUserByHostId(Long hostId);

    List<User> findAllById(List<Long> receiverAccountIds);
}