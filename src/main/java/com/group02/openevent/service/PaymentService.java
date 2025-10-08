package com.group02.openevent.service;

import com.group02.openevent.model.order.Order;
import com.group02.openevent.model.payment.Payment;
import com.group02.openevent.model.payment.PaymentStatus;
import com.group02.openevent.dto.payment.PayOSWebhookData;
import com.group02.openevent.dto.payment.PaymentResult;

import java.util.List;
import java.util.Optional;

public interface PaymentService {
    
    /**
     * Tạo payment link từ PayOS cho Order
     * @param order Order cần tạo payment
     * @param returnUrl URL trả về sau khi thanh toán thành công
     * @param cancelUrl URL trả về khi hủy thanh toán
     * @return Payment object với thông tin payment link
     */
    Payment createPaymentLinkForOrder(Order order, String returnUrl, String cancelUrl);
    
    /**
     * Lấy thông tin payment theo order
     * @param order Order cần lấy thông tin payment
     * @return Optional Payment
     */
    Optional<Payment> getPaymentByOrder(Order order);
    
    /**
     * Lấy thông tin payment theo order ID
     * @param orderId ID của order
     * @return Optional Payment
     */
    Optional<Payment> getPaymentByOrderId(Long orderId);
    
    /**
     * Xác thực webhook từ PayOS
     * @param webhookData Dữ liệu webhook từ PayOS
     * @return true nếu webhook hợp lệ
     */
    boolean verifyWebhook(PayOSWebhookData webhookData);
    
    /**
     * Xử lý webhook từ PayOS khi thanh toán thành công
     * @param webhookData Dữ liệu webhook từ PayOS
     * @return PaymentResult
     */
    PaymentResult handlePaymentWebhook(PayOSWebhookData webhookData);
    
    /**
     * Cập nhật trạng thái payment
     * @param payment Payment cần cập nhật
     * @param status Trạng thái mới
     * @param payosPaymentId PayOS Payment ID (nếu có)
     */
    void updatePaymentStatus(Payment payment, PaymentStatus status, Long payosPaymentId);
    
    /**
     * Lấy danh sách payments theo customer ID
     * @param customerId ID của customer
     * @return List Payment
     */
    List<Payment> getPaymentsByCustomerId(Long customerId);
    
    /**
     * Lấy danh sách payments theo customer ID và status
     * @param customerId ID của customer
     * @param status Trạng thái payment
     * @return List Payment
     */
    List<Payment> getPaymentsByCustomerIdAndStatus(Long customerId, PaymentStatus status);
    
    /**
     * Hủy payment (nếu chưa thanh toán)
     * @param payment Payment cần hủy
     * @return true nếu hủy thành công
     */
    boolean cancelPayment(Payment payment);
    
    /**
     * Kiểm tra và cập nhật payments hết hạn
     */
    void updateExpiredPayments();
    
    /**
     * Xử lý webhook từ PayOS SDK
     * @param webhookData Dữ liệu webhook từ PayOS SDK
     * @return PaymentResult
     */
    PaymentResult handlePaymentWebhookFromPayOS(PayOSWebhookData webhookData);
}
