package com.group02.openevent.service;


import com.group02.openevent.dto.payment.PayoutRequestDto;
import com.group02.openevent.exception.WalletException;
import com.group02.openevent.model.payment.PayoutRequest;

public interface IPayoutService {

    /**
     * Xử lý yêu cầu rút tiền từ Host.
     * 1. Kiểm tra số dư & Trừ tiền tạm thời.
     * 2. Ghi log Payout Request.
     * 3. Gọi PayOS Payout API.
     * @param hostId ID của Host
     * @param requestDto Dữ liệu yêu cầu rút tiền
     * @return PayoutRequest đã được lưu
     * @throws WalletException Nếu số dư không đủ hoặc lỗi PayOS API
     */
    PayoutRequest processPayoutRequest(Long hostId, PayoutRequestDto requestDto) throws WalletException;

    /**
     * Xử lý Webhook từ PayOS cho giao dịch Payout.
     * 1. Xác thực Checksum an toàn.
     * 2. Cập nhật trạng thái Payout Request (SUCCESS/FAILURE).
     * 3. Hoàn lại tiền nếu giao dịch PayOS thất bại.
     * @param rawWebhookBody Chuỗi JSON Body nguyên thủy của Webhook
     * @return boolean Trạng thái xử lý thành công
     */
    boolean handlePayoutWebhook(String rawWebhookBody);
}