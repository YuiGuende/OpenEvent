package com.group02.openevent.service;

import com.group02.openevent.model.account.Account;
import com.group02.openevent.model.user.Customer;
import com.group02.openevent.model.user.User;

import java.util.Optional;

public interface UserService {
    User getOrCreateUser(Account account);
    Optional<User> getUserByAccountId(Long accountId);
    Optional<Customer> getCustomerByAccountId(Long accountId);
    User createUserFromAccount(Account account);
}