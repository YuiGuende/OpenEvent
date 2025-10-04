package com.group02.openevent.repository;

import com.group02.openevent.model.event.Event;
import com.group02.openevent.model.order.Order;
import com.group02.openevent.model.user.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface IOrderRepo extends JpaRepository<Order, Long> {
    
    List<Order> findByCustomer(Customer customer);
    
    @Query("SELECT o FROM Order o WHERE o.customer.customerId = :customerId")
    List<Order> findByCustomerId(@Param("customerId") Long customerId);
    
    @Query("SELECT o FROM Order o WHERE o.event.id = :eventId")
    List<Order> findByEventId(@Param("eventId") Long eventId);

    @Query("SELECT COUNT(o) FROM Order o " +
            "WHERE o.event.id = :eventId " +
            "AND o.status = com.group02.openevent.model.order.OrderStatus.CONFIRMED")
    Integer countConfirmedParticipantsByEventId(@Param("eventId") Long eventId);

    @Query("SELECT DISTINCT o.event FROM Order o WHERE o.customer.customerId = :customerId")
    List<Event> findEventsByCustomerId(@Param("customerId") Long customerId);

    @Query("SELECT DISTINCT o.event FROM Order o " +
            "WHERE o.customer.customerId = :customerId " +
            "AND o.status = com.group02.openevent.model.order.OrderStatus.CONFIRMED")
    List<Event> findConfirmedEventsByCustomerId(@Param("customerId") Long customerId);
}


