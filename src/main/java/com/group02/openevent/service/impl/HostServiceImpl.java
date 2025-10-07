package com.group02.openevent.service.impl;

import com.group02.openevent.model.user.Host;
import com.group02.openevent.repository.IHostRepo;
import com.group02.openevent.service.HostService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional
public class HostServiceImpl implements HostService {
    
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
}