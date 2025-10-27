package com.group02.openevent.service;

import com.group02.openevent.model.user.Customer;

import java.util.Optional;

public interface CustomerService {
    Optional<Customer> findByUserId(Long userId);
    Customer save(Customer customer);
    Customer getOrCreateByUserId(Long userId);
}