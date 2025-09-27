package com.group02.openevent.repository;

import com.group02.openevent.model.order.Order;
import com.group02.openevent.model.order.OrderStatus;
import com.group02.openevent.model.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IOrderRepo extends JpaRepository<Order, Long> {
    
    // Tìm order theo order code
    Optional<Order> findByOrderCode(String orderCode);
    
    // Tìm orders theo user
    List<Order> findByUser(User user);
    
    // Tìm orders theo user và status
    List<Order> findByUserAndStatus(User user, OrderStatus status);
    
    // Tìm orders theo status
    List<Order> findByStatus(OrderStatus status);
    
    // Tìm orders theo user ID
    @Query("SELECT o FROM Order o WHERE o.user.userId = :userId ORDER BY o.createdAt DESC")
    List<Order> findByUserId(@Param("userId") Long userId);
    
    // Tìm orders theo user ID và status
    @Query("SELECT o FROM Order o WHERE o.user.userId = :userId AND o.status = :status ORDER BY o.createdAt DESC")
    List<Order> findByUserIdAndStatus(@Param("userId") Long userId, @Param("status") OrderStatus status);
    
    // Đếm số orders theo user và status
    long countByUserAndStatus(User user, OrderStatus status);
    
    // Tìm orders đang pending (chưa thanh toán)
    @Query("SELECT o FROM Order o WHERE o.status = 'PENDING' AND o.createdAt < :expiredTime")
    List<Order> findExpiredPendingOrders(@Param("expiredTime") java.time.LocalDateTime expiredTime);
}
