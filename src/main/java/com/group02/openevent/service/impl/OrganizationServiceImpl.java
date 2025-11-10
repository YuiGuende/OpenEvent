package com.group02.openevent.service.impl;

import com.group02.openevent.model.organization.Organization;
import com.group02.openevent.repository.IOrganizationRepo;
import com.group02.openevent.service.OrganizationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class OrganizationServiceImpl implements OrganizationService {
    
    private final IOrganizationRepo organizationRepo;
    
    public OrganizationServiceImpl(IOrganizationRepo organizationRepo) {
        this.organizationRepo = organizationRepo;
    }
    
    @Override
    public Optional<Organization> findById(Long id) {
        return organizationRepo.findById(id);
    }
    
    @Override
    public Organization save(Organization organization) {
        return organizationRepo.save(organization);
    }

    @Override
    public List<Organization> findAll() {
        return organizationRepo.findAll();
    }
}