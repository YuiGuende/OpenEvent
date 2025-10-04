package com.group02.openevent.repository;

import com.group02.openevent.model.payment.Payment;
import com.group02.openevent.model.payment.PaymentStatus;
import com.group02.openevent.model.order.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IPaymentRepo extends JpaRepository<Payment, Long> {
    
    // Tìm payment theo order
    Optional<Payment> findByOrder(Order order);
    
    // Tìm payment theo order ID
    Optional<Payment> findByOrder_OrderId(Long orderId);
    
    // Tìm payment theo PayOS payment ID
    Optional<Payment> findByPayosPaymentId(Long payosPaymentId);
    
    // Tìm payment theo payment link ID
    Optional<Payment> findByPaymentLinkId(String paymentLinkId);
    
    // Tìm payments theo status
    List<Payment> findByStatus(PaymentStatus status);
    
    // Tìm payments theo user
    @Query("SELECT p FROM Payment p WHERE p.order.customer.customerId = :userId ORDER BY p.createdAt DESC")
    List<Payment> findByOrder_User_UserId(@Param("userId") Long userId);
    
    // Tìm payments theo user và status
    @Query("SELECT p FROM Payment p WHERE p.order.customer.customerId = :userId AND p.status = :status ORDER BY p.createdAt DESC")
    List<Payment> findByOrder_User_UserIdAndStatus(@Param("userId") Long userId, @Param("status") PaymentStatus status);
    
    // Tìm payments đang pending (chưa thanh toán)
    @Query("SELECT p FROM Payment p WHERE p.status = 'PENDING' AND p.expiredAt < :currentTime")
    List<Payment> findExpiredPendingPayments(@Param("currentTime") java.time.LocalDateTime currentTime);
    
    // Đếm số payments theo user và status
    @Query("SELECT COUNT(p) FROM Payment p WHERE p.order.customer.customerId = :userId AND p.status = :status")
    long countByUserIdAndStatus(@Param("userId") Long userId, @Param("status") PaymentStatus status);
}
