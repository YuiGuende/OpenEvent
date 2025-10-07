package com.group02.openevent.service;

import com.group02.openevent.model.user.Host;

import java.util.Optional;

public interface HostService {
    Optional<Host> findByCustomerId(Long customerId);
    Host save(Host host);
}