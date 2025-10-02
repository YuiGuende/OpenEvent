//package com.group02.openevent.service;
//
//import com.group02.openevent.model.ticket.Ticket;
//import com.group02.openevent.model.payment.Payment;
//import com.group02.openevent.model.payment.PaymentStatus;
//import com.group02.openevent.dto.payment.PayOSWebhookData;
//import com.group02.openevent.dto.payment.PaymentResult;
//import vn.payos.type.WebhookData;
//
//import java.util.List;
//import java.util.Optional;
//
//public interface PaymentService {
//
//    /**
//     * Tạo payment link từ PayOS
//     * @param ticket Ticket cần tạo payment
//     * @param returnUrl URL trả về sau khi thanh toán thành công
//     * @param cancelUrl URL trả về khi hủy thanh toán
//     * @return Payment object với thông tin payment link
//     */
//    Payment createPaymentLink(Ticket ticket, String returnUrl, String cancelUrl);
//
//    /**
//     * Lấy thông tin payment theo ticket
//     * @param ticket Ticket cần lấy thông tin payment
//     * @return Optional Payment
//     */
//    Optional<Payment> getPaymentByTicket(Ticket ticket);
//
//    /**
//     * Lấy thông tin payment theo ticket ID
//     * @param ticketId ID của ticket
//     * @return Optional Payment
//     */
//    Optional<Payment> getPaymentByTicketId(Long ticketId);
//
//    /**
//     * Xác thực webhook từ PayOS
//     * @param webhookData Dữ liệu webhook từ PayOS
//     * @return true nếu webhook hợp lệ
//     */
//    boolean verifyWebhook(PayOSWebhookData webhookData);
//
//    /**
//     * Xử lý webhook từ PayOS khi thanh toán thành công
//     * @param webhookData Dữ liệu webhook từ PayOS
//     * @return PaymentResult
//     */
//    PaymentResult handlePaymentWebhook(PayOSWebhookData webhookData);
//
//    /**
//     * Cập nhật trạng thái payment
//     * @param payment Payment cần cập nhật
//     * @param status Trạng thái mới
//     * @param payosPaymentId PayOS Payment ID (nếu có)
//     */
//    void updatePaymentStatus(Payment payment, PaymentStatus status, Long payosPaymentId);
//
//    /**
//     * Lấy danh sách payments theo user ID
//     * @param userId ID của user
//     * @return List Payment
//     */
//    List<Payment> getPaymentsByUserId(Long userId);
//
//    /**
//     * Lấy danh sách payments theo user ID và status
//     * @param userId ID của user
//     * @param status Trạng thái payment
//     * @return List Payment
//     */
//    List<Payment> getPaymentsByUserIdAndStatus(Long userId, PaymentStatus status);
//
//    /**
//     * Hủy payment (nếu chưa thanh toán)
//     * @param payment Payment cần hủy
//     * @return true nếu hủy thành công
//     */
//    boolean cancelPayment(Payment payment);
//
//    /**
//     * Kiểm tra và cập nhật payments hết hạn
//     */
//    void updateExpiredPayments();
//
//    /**
//     * Xử lý webhook từ PayOS SDK
//     * @param webhookData Dữ liệu webhook từ PayOS SDK
//     * @return PaymentResult
//     */
//    PaymentResult handlePaymentWebhookFromPayOS(PayOSWebhookData webhookData);
//}
