package com.group02.openevent.repository;

import com.group02.openevent.model.order.Order;
import com.group02.openevent.model.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface IOrderRepo extends JpaRepository<Order, Long> {
    
    List<Order> findByUser(User user);
    
    @Query("SELECT o FROM Order o WHERE o.user.userId = :userId")
    List<Order> findByUserId(@Param("userId") Long userId);
    
    @Query("SELECT o FROM Order o WHERE o.event.id = :eventId")
    List<Order> findByEventId(@Param("eventId") Long eventId);
}


