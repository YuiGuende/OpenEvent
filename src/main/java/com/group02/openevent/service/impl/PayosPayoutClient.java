package com.group02.openevent.service.impl;

import com.group02.openevent.model.payment.PayoutRequest;
import org.springframework.stereotype.Service;
import vn.payos.PayOS;
import vn.payos.model.v1.payouts.Payout;
import vn.payos.model.v1.payouts.PayoutRequests;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class PayosPayoutClient {

    private static final Logger logger = LoggerFactory.getLogger(PayosPayoutClient.class);
    private final PayOS payOS;

    public PayosPayoutClient(PayOS payOS) {
        this.payOS = payOS;
    }

    /**
     * Gửi yêu cầu Payout đến PayOS sử dụng SDK.
     *
     * @param request PayoutRequest Entity chứa dữ liệu
     * @return String Mã giao dịch PayOS (Payout ID)
     * @throws Exception Nếu có lỗi từ PayOS SDK/API
     */
    public String sendPayout(PayoutRequest request) throws Exception { // Sử dụng Exception

        // 1. Ánh xạ Entity sang SDK DTO PayoutRequests
        PayoutRequests payoutRequestsBody = PayoutRequests.builder()
                .referenceId(request.getPayosOrderCode())
                .amount(request.getAmount().longValue())
                .toBin(request.getBankCode())
                .toAccountNumber(request.getBankAccountNumber())
                .description("Rut tien Host #" + request.getPayosOrderCode())
                .build();

        try {
            logger.info("Sending Payout request via SDK for referenceId: {}", payoutRequestsBody.getReferenceId());

            // 2. Gọi API Payout: SDK sẽ tự động tính Checksum và gửi
            Payout payoutResponse = payOS.payouts().create(payoutRequestsBody);

            // 3. Trả về Payout ID/Transaction ID của PayOS
            if (payoutResponse != null && payoutResponse.getId() != null) {
                return payoutResponse.getId();
            } else {
                // Ném ra Exception chung nếu không nhận được ID
                throw new Exception("PayOS Payout creation failed: Null response or Payout ID.");
            }
        } catch (Exception e) { // Bắt Exception chung
            logger.error("PayOS Payout Error for {}: {}", payoutRequestsBody.getReferenceId(), e.getMessage());
            // Ném lại Exception để được bắt trong Service Layer
            throw e;
        }
    }

    /**
     * Xác thực Webhook Payout nhận được từ PayOS.
     *
     * @param rawWebhookBody Chuỗi JSON Body nguyên thủy của Webhook
     * @return boolean True nếu Checksum hợp lệ
     */
    public boolean verifyWebhookChecksum(String rawWebhookBody) {
        try {
            payOS.webhooks().verify(rawWebhookBody);
            logger.info("Payout Webhook Checksum verified successfully.");
            return true;
        } catch (Exception e) { // Bắt Exception chung
            logger.error("SECURITY ALERT: Payout Webhook Checksum Verification Failed: {}", e.getMessage());
            return false;
        }
    }
}