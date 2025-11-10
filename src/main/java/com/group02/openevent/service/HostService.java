package com.group02.openevent.service;

import com.group02.openevent.dto.request.HostRegistrationRequest;
import com.group02.openevent.model.user.Customer;
import com.group02.openevent.model.user.Host;

import java.util.Optional;

public interface HostService {
    Optional<Host> findByCustomerId(Long customerId);
    Host save(Host host);
    public Long findHostIdByAccountId(Long accountId);
    boolean isCustomerHost(Long customerId);
    Host registerHost(Customer customer, HostRegistrationRequest request);
}