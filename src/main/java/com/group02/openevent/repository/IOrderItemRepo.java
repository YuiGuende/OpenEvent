package com.group02.openevent.repository;

import com.group02.openevent.model.order.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface IOrderItemRepo extends JpaRepository<OrderItem, Long> {
    
    List<OrderItem> findByOrder_OrderId(Long orderId);
    
    @Query("SELECT oi FROM OrderItem oi WHERE oi.order.user.userId = :userId")
    List<OrderItem> findByUserId(@Param("userId") Long userId);
    
    @Query("SELECT oi FROM OrderItem oi WHERE oi.ticketType.event.id = :eventId")
    List<OrderItem> findByEventId(@Param("eventId") Long eventId);
}
