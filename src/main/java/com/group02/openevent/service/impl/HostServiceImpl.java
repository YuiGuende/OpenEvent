package com.group02.openevent.service.impl;

import com.group02.openevent.model.user.Host;
import com.group02.openevent.repository.IHostRepo;
import com.group02.openevent.service.HostService;
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
    public Long findHostIdByAccountId(Long accountId) {
        Host host = hostRepo.findByAccountId(accountId)
                .orElseThrow(() -> new RuntimeException("This account is not a host"));
        return host.getId();
    }
}