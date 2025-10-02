package com.group02.openevent.model.ticket;

public enum TicketStatus {
    PENDING,        // Chờ thanh toán
    PAID,          // Đã thanh toán (có vé)
    CANCELLED,     // Đã hủy
    REFUNDED,      // Đã hoàn tiền
    EXPIRED        // Hết hạn
}
