package com.group02.openevent.repository;

import com.group02.openevent.model.organization.Organization;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IOrganizationRepo extends JpaRepository<Organization, Long> {

}
