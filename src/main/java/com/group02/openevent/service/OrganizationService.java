package com.group02.openevent.service;

import com.group02.openevent.model.organization.Organization;

import java.util.List;
import java.util.Optional;

public interface OrganizationService {
    Optional<Organization> findById(Long id);
    Organization save(Organization organization);
    List<Organization> findAll();
}