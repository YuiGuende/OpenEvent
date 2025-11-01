package com.group02.openevent.repository;


import com.group02.openevent.model.department.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface IDepartmentRepo extends JpaRepository<Department, Long> {
    
    Optional<Department> findByAccountId(Long accountId);
    
    Optional<Department> findByDepartmentName(String departmentName);
}
