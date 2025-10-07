package com.group02.openevent.repository;

import com.group02.openevent.model.organization.Organization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface IOrganizationRepo extends JpaRepository<Organization, Long> {
    Optional<Organization> findById(Long id);
}