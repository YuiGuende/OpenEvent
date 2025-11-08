package com.group02.openevent.service.impl;

import com.group02.openevent.model.user.Host;
import com.group02.openevent.repository.IHostRepo;
import com.group02.openevent.service.HostService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional
public class HostServiceImpl implements HostService {
    @Autowired
    private final IHostRepo hostRepo;

    public HostServiceImpl(IHostRepo hostRepo) {
        this.hostRepo = hostRepo;
    }

    @Override
    public Optional<Host> findByCustomerId(Long customerId) {
        return hostRepo.findByCustomer_CustomerId(customerId);
    }

    @Override
    public Host save(Host host) {
        return hostRepo.save(host);
    }

    @Override
    public Host findHostByAccountId(Long accountId) {
        return hostRepo.findByUser_UserId(accountId)
                .orElseThrow(() -> new RuntimeException("This account is not a host"));

    }

    @Override
    public Host getHostFromSession(HttpSession session) {
        Long accountId = (Long) session.getAttribute("USER_ID");
        if (accountId == null) {
            throw new RuntimeException("User not logged in");
        }
        return findHostByAccountId(accountId);
    }
}