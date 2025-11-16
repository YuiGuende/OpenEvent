package com.group02.openevent.config;// src/main/java/com/group02/openevent.config.PayOSPayoutConfig.java

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import vn.payos.PayOS;

@Configuration
@ConditionalOnProperty(name = "payos.payout-client-id", matchIfMissing = false)
public class PayOSPayoutConfig {

    // Lấy các khóa API riêng cho Payout
    @Value("${payos.payout-client-id}")
    private String payoutClientId;

    @Value("${payos.payout-api-key}")
    private String payoutApiKey;

    @Value("${payos.payout-checksum-key}")
    private String payoutChecksumKey;

    /**
     * Khởi tạo PayOS Bean chuyên dụng cho Payout.
     * Bean này cần được inject vào PayoutController và PayosPayoutClient.
     */
    @Bean("payOSPayout") // Đặt tên Bean khác với Bean Payment (payOS)
    public PayOS payOSPayout() {
        // Log để kiểm tra các giá trị có được load không
        System.out.println("Initializing PayOS Payout Client. ID: " + payoutClientId); 
        
        // Kiểm tra lỗi: Nếu 1 trong 3 khóa rỗng, PayOS sẽ báo lỗi "API key không tồn tại"
        if (payoutClientId == null || payoutClientId.isEmpty() ||
            payoutApiKey == null || payoutApiKey.isEmpty() ||
            payoutChecksumKey == null || payoutChecksumKey.isEmpty()) {
            throw new RuntimeException("PayOS Payout keys are missing or empty in application properties.");
        }
        
        return new PayOS(payoutClientId, payoutApiKey, payoutChecksumKey);
    }
}