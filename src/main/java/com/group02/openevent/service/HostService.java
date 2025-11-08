package com.group02.openevent.service;

import com.group02.openevent.model.user.Host;
import jakarta.servlet.http.HttpSession;

import java.util.Optional;

public interface HostService {
    Optional<Host> findByCustomerId(Long customerId);
    Host save(Host host);
    Host findHostByAccountId(Long accountId);

    Host getHostFromSession(HttpSession session);
}