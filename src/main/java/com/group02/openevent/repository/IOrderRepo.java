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

    @Query("SELECT o FROM Order o WHERE o.event.department.user.account.accountId = :departmentId")
    List<Order> findByDepartmentId(@Param("departmentId") Long departmentId);

    @Query("SELECT o FROM Order o WHERE o.event.department.user.account.accountId = :departmentId AND o.status = :status")
    Page<Order> findByDepartmentIdAndStatus(@Param("departmentId") Long departmentId,
                                            @Param("status") OrderStatus status,
                                            Pageable pageable);

    @Query("SELECT o FROM Order o WHERE o.event.department.user.account.accountId = :departmentId")
    Page<Order> findByDepartmentId(@Param("departmentId") Long departmentId, Pageable pageable);

    @Query("SELECT o FROM Order o WHERE o.event.id = :eventId AND o.status = :status")
    Page<Order> findByEventIdAndStatus(@Param("eventId") Long eventId,
                                       @Param("status") OrderStatus status,
                                       Pageable pageable);

    @Query("SELECT o FROM Order o WHERE o.event.id = :eventId")
    Page<Order> findByEventId(@Param("eventId") Long eventId, Pageable pageable);

    @Query("SELECT DISTINCT o.customer.user.account.accountId FROM Order o " +
            "WHERE o.event.id = :eventId AND o.status = 'PAID'")
    List<Long> findDistinctCustomerAccountIdsByEventIdAndStatusPaid(@Param("eventId") Long eventId);

    boolean existsByTicketType_TicketTypeId(Long ticketTypeId);

    /**
     * Check if there exists a PAID order for given event and participant email
     */
    @Query("SELECT CASE WHEN COUNT(o) > 0 THEN true ELSE false END FROM Order o " +
            "WHERE o.event.id = :eventId " +
            "AND o.status = com.group02.openevent.model.order.OrderStatus.PAID " +
            "AND LOWER(o.participantEmail) = LOWER(:email)")
    boolean existsPaidByEventIdAndParticipantEmail(@Param("eventId") Long eventId, @Param("email") String email);

    /**
     * Check if there exists a PAID order for given event and customer
     */
    @Query("SELECT CASE WHEN COUNT(o) > 0 THEN true ELSE false END FROM Order o " +
            "WHERE o.event.id = :eventId " +
            "AND o.status = com.group02.openevent.model.order.OrderStatus.PAID " +
            "AND o.customer.customerId = :customerId")
        boolean existsPaidByEventIdAndCustomerId(@Param("eventId") Long eventId, @Param("customerId") Long customerId);

    /**
     * Find all PAID orders for events belonging to a specific host
     */
    @Query("SELECT o FROM Order o WHERE o.event.host.id = :hostId " +
            "AND o.status = com.group02.openevent.model.order.OrderStatus.PAID")
    List<Order> findByHostIdAndStatusPaid(@Param("hostId") Long hostId);
    
    /**
     * Find all PAID orders for events belonging to a specific host with pagination
     */
    @Query("SELECT o FROM Order o WHERE o.event.host.id = :hostId " +
            "AND o.status = com.group02.openevent.model.order.OrderStatus.PAID " +
            "ORDER BY o.createdAt DESC")
    Page<Order> findByHostIdAndStatusPaid(@Param("hostId") Long hostId, Pageable pageable);
    
    /**
     * Find all orders for events belonging to a specific host (all statuses)
     */
    @Query("SELECT o FROM Order o WHERE o.event.host.id = :hostId " +
            "ORDER BY o.createdAt DESC")
    List<Order> findByHostId(@Param("hostId") Long hostId);
    
    /**
     * Find all orders for events belonging to a specific host with pagination
     */
    @Query("SELECT o FROM Order o WHERE o.event.host.id = :hostId " +
            "ORDER BY o.createdAt DESC")
    Page<Order> findByHostId(@Param("hostId") Long hostId, Pageable pageable);
    
    /**
     * Find pending orders created before a specific time
     */
    @Query("SELECT o FROM Order o WHERE o.status = com.group02.openevent.model.order.OrderStatus.PENDING " +
            "AND o.createdAt < :beforeTime")
    List<Order> findPendingOrdersCreatedBefore(@Param("beforeTime") java.time.LocalDateTime beforeTime);
}


