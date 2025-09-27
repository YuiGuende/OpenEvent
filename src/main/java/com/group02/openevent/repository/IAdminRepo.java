package com.group02.openevent.repository;

import com.group02.openevent.model.admin.Admin;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IAdminRepo extends JpaRepository<Admin, Long> {
} 