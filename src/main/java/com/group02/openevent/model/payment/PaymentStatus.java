package com.group02.openevent.model.payment;

public enum PaymentStatus {
    PENDING,        // Chờ thanh toán
    PAID,          // Đã thanh toán
    CANCELLED,     // Đã hủy
    EXPIRED,       // Hết hạn
    REFUNDED       // Đã hoàn tiền
}
