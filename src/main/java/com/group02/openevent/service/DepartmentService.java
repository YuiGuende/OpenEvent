package com.group02.openevent.service;



import com.group02.openevent.dto.department.DepartmentStatsDTO;
import com.group02.openevent.model.department.Department;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface DepartmentService {

    Department getDepartmentByAccountId(Long accountId);

    Department saveDepartment(Department department);

    DepartmentStatsDTO getDepartmentStats(Long departmentId);

    Map<String, Object> getEventsByMonth(Long departmentId);

    Map<String, Object> getEventsByType(Long departmentId);

    Map<String, Object> getParticipantsTrend(Long departmentId);
}
