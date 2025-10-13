package com.group02.openevent.dto.payment;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * DTO mô phỏng cấu trúc Webhook nhận được từ PayOS sau khi Payout được xử lý.
 * Lớp này được sử dụng SAU KHI JSON Body đã được xác thực Checksum.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PayoutWebhookData {

    // Đây là trường chứa dữ liệu giao dịch thực tế
    @JsonProperty("data")
    private PayoutWebhookDetail data;

    // Trường chứa Signature/Checksum được PayOS gửi kèm (dùng để xác thực)
    // Lưu ý: Trường này có thể nằm ở Header hoặc Body tùy cấu hình Webhook.
    @JsonProperty("signature")
    private String signature; 

    // Trường type của Webhook (ví dụ: 'payout_success', 'payout_failure')
    @JsonProperty("type")
    private String type; 

    // --- Cần định nghĩa lớp Detail chứa các thông tin quan trọng ---

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PayoutWebhookDetail {
        
        // Mã Reference ID bạn đã gửi khi tạo Payout
        @JsonProperty("referenceId")
        private String referenceId; 
        
        // Trạng thái giao dịch (SUCCESS, FAILURE)
        @JsonProperty("status")
        private String status; 
        
        // Mã Payout ID (transactionId) của PayOS
        @JsonProperty("payoutId")
        private String payoutId; 

        // Số tiền Payout
        @JsonProperty("amount")
        private Long amount;
        
        // Thời gian xử lý
        @JsonProperty("paidAt")
        private Long paidAt; 
        
        // Mã lỗi nếu thất bại
        @JsonProperty("errorCode")
        private String errorCode; 
    }
    
    // Phương thức tiện ích để lấy Mã Order Code (Reference ID)
    public String getOrderCode() {
        return this.data != null ? this.data.getReferenceId() : null;
    }
    
    // Phương thức tiện ích để lấy Trạng thái
    public String getStatus() {
        return this.data != null ? this.data.getStatus() : null;
    }
    
    // Phương thức tiện ích để lấy Transaction ID
    public String getTransactionId() {
        return this.data != null ? this.data.getPayoutId() : null;
    }
}