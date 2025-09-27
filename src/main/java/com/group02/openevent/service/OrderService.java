package com.group02.openevent.service;

import com.group02.openevent.model.order.Order;
import com.group02.openevent.model.order.OrderStatus;
import com.group02.openevent.model.user.User;
import com.group02.openevent.dto.order.CreateOrderRequest;
import com.group02.openevent.dto.order.OrderResponse;

import java.util.List;
import java.util.Optional;

public interface OrderService {
    
    /**
     * Tạo order mới
     * @param request Thông tin tạo order
     * @param user User tạo order
     * @return Order đã tạo
     */
    Order createOrder(CreateOrderRequest request, User user);
    
    /**
     * Lấy order theo ID
     * @param orderId ID của order
     * @return Optional Order
     */
    Optional<Order> getOrderById(Long orderId);
    
    /**
     * Lấy order theo order code
     * @param orderCode Mã order
     * @return Optional Order
     */
    Optional<Order> getOrderByCode(String orderCode);
    
    /**
     * Lấy danh sách orders theo user
     * @param user User cần lấy orders
     * @return List Order
     */
    List<Order> getOrdersByUser(User user);
    
    /**
     * Lấy danh sách orders theo user ID
     * @param userId ID của user
     * @return List Order
     */
    List<Order> getOrdersByUserId(Long userId);
    
    /**
     * Lấy danh sách orders theo user ID và status
     * @param userId ID của user
     * @param status Trạng thái order
     * @return List Order
     */
    List<Order> getOrdersByUserIdAndStatus(Long userId, OrderStatus status);
    
    /**
     * Cập nhật trạng thái order
     * @param order Order cần cập nhật
     * @param status Trạng thái mới
     */
    void updateOrderStatus(Order order, OrderStatus status);
    
    /**
     * Hủy order (chỉ khi chưa thanh toán)
     * @param order Order cần hủy
     * @return true nếu hủy thành công
     */
    boolean cancelOrder(Order order);
    
    /**
     * Tạo order code duy nhất
     * @return Order code
     */
    String generateOrderCode();
    
    /**
     * Kiểm tra và cập nhật orders hết hạn
     */
    void updateExpiredOrders();
    
    /**
     * Lấy thống kê orders theo user
     * @param userId ID của user
     * @return OrderResponse chứa thống kê
     */
    OrderResponse getOrderStatistics(Long userId);
    
    /**
     * Kiểm tra user đã đăng ký event chưa
     * @param userId ID của user
     * @param eventId ID của event
     * @return true nếu đã đăng ký
     */
    boolean hasUserRegisteredEvent(Long userId, Long eventId);
}
