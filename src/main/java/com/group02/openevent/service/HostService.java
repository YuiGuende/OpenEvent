package com.group02.openevent.service;

import com.group02.openevent.dto.request.HostRegistrationRequest;
import com.group02.openevent.model.user.Customer;
import com.group02.openevent.model.user.Host;
import com.group02.openevent.model.user.User;
import jakarta.servlet.http.HttpSession;

public interface HostService {
    Host findByCustomerId(Long customerId);
    Host save(Host host);
    boolean isUserHost(Long userId);
    Host registerHost(User user, HostRegistrationRequest request); // Đổi từ Customer sang User
    Host findHostByUserId(Long userId);

    Host getHostFromSession(HttpSession session);
}