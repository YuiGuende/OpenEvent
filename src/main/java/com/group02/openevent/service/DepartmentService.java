package com.group02.openevent.service;



import com.group02.openevent.dto.department.DepartmentStatsDTO;
import com.group02.openevent.dto.department.FeaturedEventDTO;
import com.group02.openevent.dto.department.OrderDTO;
import com.group02.openevent.model.department.Department;
import com.group02.openevent.model.order.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;


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


    Map<String, Object> getRevenueTrend(Long departmentId);

    List<FeaturedEventDTO> getFeaturedEvents(Long departmentId, int limit);

    Page<OrderDTO> getOrdersByDepartment(Long departmentId, OrderStatus status, Pageable pageable);

    Double getAverageApprovalTime(Long departmentId);

    Map<String, Object> getApprovalTrendData(Long departmentId);

    Map<String, Object> getRevenueTrendData(Long departmentId);

    Map<String, Object> getOrderStatusDistribution(Long departmentId);

}
