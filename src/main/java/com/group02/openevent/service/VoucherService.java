package com.group02.openevent.service;

import com.group02.openevent.model.voucher.Voucher;
import com.group02.openevent.model.voucher.VoucherUsage;
import com.group02.openevent.model.order.Order;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface VoucherService {
    
    /**
     * Tạo voucher mới
     */
    Voucher createVoucher(Voucher voucher);
    
    /**
     * Lấy voucher theo code
     */
    Optional<Voucher> getVoucherByCode(String code);
    
    /**
     * Kiểm tra voucher có khả dụng không
     */
    boolean isVoucherAvailable(String code);
    
    /**
     * Áp dụng voucher vào order
     */
    VoucherUsage applyVoucherToOrder(String voucherCode, Order order);
    
    /**
     * Tính toán discount amount cho voucher
     */
    BigDecimal calculateVoucherDiscount(String voucherCode, BigDecimal originalPrice);
    
    /**
     * Lấy tất cả voucher khả dụng
     */
    List<Voucher> getAvailableVouchers();
    
    /**
     * Lấy lịch sử sử dụng voucher
     */
    List<VoucherUsage> getVoucherUsageHistory(Long voucherId);
    
    /**
     * Hủy voucher
     */
    boolean disableVoucher(Long voucherId);
    
    /**
     * Cập nhật số lượng voucher
     */
    Voucher updateVoucherQuantity(Long voucherId, Integer newQuantity);
}
