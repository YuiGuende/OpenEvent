package com.group02.openevent.service;


import com.group02.openevent.exception.WalletException;
import com.group02.openevent.model.user.HostWallet;

import java.math.BigDecimal;

public interface IHostWalletService {
    
    /**
     * Lấy thông tin ví của Host (tạo mới nếu chưa tồn tại).
     * @param hostId ID của Host
     * @return HostWallet Entity
     */
    HostWallet getWalletByHostId(Long hostId);

    /**
     * Trừ tiền tạm thời khỏi ví Host khi có yêu cầu Payout.
     * Đồng thời ghi lại giao dịch PENDING vào WalletTransaction.
     * @param hostId ID của Host
     * @param amount Số tiền cần trừ
     * @param referenceId Mã tham chiếu (PayOS Order Code)
     * @param description Mô tả giao dịch
     * @throws WalletException Nếu số dư không đủ
     */
    void deductBalance(Long hostId, BigDecimal amount, String referenceId, String description) throws WalletException;

    /**
     * Cộng tiền lại vào ví Host (Hoàn lại tiền sau khi Payout thất bại hoặc bị hủy).
     * @param hostId ID của Host
     * @param amount Số tiền cần hoàn lại
     */
    void refundBalance(Long hostId, BigDecimal amount);

    /**
     * Cộng tiền vào ví Host khi có order thành công (thanh toán thành công).
     * @param hostId ID của Host
     * @param amount Số tiền cần cộng
     * @param referenceId Mã tham chiếu (Order ID)
     * @param description Mô tả giao dịch
     */
    void addBalance(Long hostId, BigDecimal amount, String referenceId, String description);
}