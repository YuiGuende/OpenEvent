package com.group02.openevent.model.order;

public enum OrderStatus {
    PENDING,        // Chờ thanh toán
    PAID,          // Đã thanh toán
    CANCELLED,     // Đã hủy
    REFUNDED,      // Đã hoàn tiền
    EXPIRED        // Hết hạn
}
