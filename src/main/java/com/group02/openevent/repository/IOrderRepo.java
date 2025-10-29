package com.group02.openevent.repository;

import com.group02.openevent.model.event.Event;
import com.group02.openevent.model.order.Order;
import com.group02.openevent.model.order.OrderStatus;
import com.group02.openevent.model.user.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Service;


import java.util.List;
@Service
public interface IOrderRepo extends JpaRepository<Order, Long> {
    
    List<Order> findByCustomer(Customer customer);
    
    @Query("SELECT o FROM Order o WHERE o.customer.customerId = :customerId")
    List<Order> findByCustomerId(@Param("customerId") Long customerId);
    
    @Query("SELECT o FROM Order o WHERE o.event.id = :eventId")
    List<Order> findByEventId(@Param("eventId") Long eventId);

    @Query("SELECT COUNT(o) FROM Order o " +
            "WHERE o.event.id = :eventId " +
            "AND o.status = com.group02.openevent.model.order.OrderStatus.PAID")
    Integer countConfirmedParticipantsByEventId(@Param("eventId") Long eventId);

    @Query("SELECT DISTINCT o.event FROM Order o WHERE o.customer.customerId = :customerId")
    List<Event> findEventsByCustomerId(@Param("customerId") Long customerId);

    @Query("SELECT DISTINCT o.event FROM Order o " +
            "WHERE o.customer.customerId = :customerId " +
            "AND o.status = com.group02.openevent.model.order.OrderStatus.PAID")
    List<Event> findConfirmedEventsByCustomerId(@Param("customerId") Long customerId);

    @Query("SELECT o FROM Order o WHERE o.event.department.accountId = :departmentId")
    List<Order> findByDepartmentId(@Param("departmentId") Long departmentId);

    @Query("SELECT o FROM Order o WHERE o.event.department.accountId = :departmentId AND o.status = :status")
    Page<Order> findByDepartmentIdAndStatus(@Param("departmentId") Long departmentId,
                                            @Param("status") OrderStatus status,
                                            Pageable pageable);

    @Query("SELECT o FROM Order o WHERE o.event.department.accountId = :departmentId")
    Page<Order> findByDepartmentId(@Param("departmentId") Long departmentId, Pageable pageable);

    @Query("SELECT o FROM Order o WHERE o.event.id = :eventId AND o.status = :status")
    Page<Order> findByEventIdAndStatus(@Param("eventId") Long eventId,
                                       @Param("status") OrderStatus status,
                                       Pageable pageable);
    @Query("SELECT o FROM Order o WHERE o.event.id = :eventId")
    Page<Order> findByEventId(@Param("eventId") Long eventId, Pageable pageable);

    @Query("SELECT DISTINCT o.customer.account.accountId FROM Order o " +
            "WHERE o.event.id = :eventId AND o.status = 'PAID'")
    List<Long> findDistinctCustomerAccountIdsByEventIdAndStatusPaid(@Param("eventId") Long eventId);

    // Check if any order references a given ticket type (protect FK delete)
    boolean existsByTicketType_TicketTypeId(Long ticketTypeId);
}


