package com.group02.openevent.service.impl;

import com.group02.openevent.model.voucher.Voucher;
import com.group02.openevent.model.voucher.VoucherStatus;
import com.group02.openevent.model.voucher.VoucherUsage;
import com.group02.openevent.model.order.Order;
import com.group02.openevent.repository.IVoucherRepo;
import com.group02.openevent.repository.IVoucherUsageRepo;
import com.group02.openevent.service.VoucherService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class VoucherServiceImpl implements VoucherService {
    
    @Autowired
    private IVoucherRepo voucherRepo;
    
    @Autowired
    private IVoucherUsageRepo voucherUsageRepo;
    
    @Override
    public Voucher createVoucher(Voucher voucher) {
        return voucherRepo.save(voucher);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Optional<Voucher> getVoucherByCode(String code) {
        return voucherRepo.findByCode(code);
    }
    
    @Override
    @Transactional(readOnly = true)
    public boolean isVoucherAvailable(String code) {
        Optional<Voucher> voucherOpt = voucherRepo.findAvailableVoucherByCode(code, LocalDateTime.now());
        return voucherOpt.isPresent();
    }
    
    @Override
    public VoucherUsage applyVoucherToOrder(String voucherCode, Order order) {
        Optional<Voucher> voucherOpt = voucherRepo.findAvailableVoucherByCode(voucherCode, LocalDateTime.now());
        
        if (voucherOpt.isEmpty()) {
            throw new IllegalArgumentException("Voucher không hợp lệ hoặc đã hết hạn");
        }
        
        Voucher voucher = voucherOpt.get();
        
        // Kiểm tra số lượng còn lại
        if (voucher.getQuantity() <= 0) {
            throw new IllegalArgumentException("Voucher đã hết số lượng sử dụng");
        }
        
        // Tính discount amount
        BigDecimal discountAmount = calculateVoucherDiscount(voucherCode, order.getOriginalPrice());
        
        // Tạo voucher usage record
        VoucherUsage voucherUsage = new VoucherUsage(voucher, order, discountAmount);
        voucherUsage = voucherUsageRepo.save(voucherUsage);
        
        // Cập nhật order với voucher info
        order.setVoucher(voucher);
        order.setVoucherCode(voucherCode);
        order.setVoucherDiscountAmount(discountAmount);
        order.calculateTotalAmount();
        
        // Giảm số lượng voucher
        voucher.decreaseQuantity();
        voucherRepo.save(voucher);
        
        return voucherUsage;
    }
    
    @Override
    @Transactional(readOnly = true)
    public BigDecimal calculateVoucherDiscount(String voucherCode, BigDecimal originalPrice) {
        Optional<Voucher> voucherOpt = voucherRepo.findAvailableVoucherByCode(voucherCode, LocalDateTime.now());
        
        if (voucherOpt.isEmpty()) {
            return BigDecimal.ZERO;
        }
        
        Voucher voucher = voucherOpt.get();
        BigDecimal discountAmount = voucher.getDiscountAmount();
        
        // Đảm bảo discount không vượt quá original price
        if (discountAmount.compareTo(originalPrice) > 0) {
            discountAmount = originalPrice;
        }
        
        return discountAmount;
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<Voucher> getAvailableVouchers() {
        return voucherRepo.findAllAvailableVouchers(LocalDateTime.now());
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<VoucherUsage> getVoucherUsageHistory(Long voucherId) {
        return voucherUsageRepo.findByVoucherVoucherId(voucherId);
    }
    
    @Override
    public boolean disableVoucher(Long voucherId) {
        Optional<Voucher> voucherOpt = voucherRepo.findById(voucherId);
        if (voucherOpt.isPresent()) {
            Voucher voucher = voucherOpt.get();
            voucher.setStatus(VoucherStatus.DISABLED);
            voucherRepo.save(voucher);
            return true;
        }
        return false;
    }
    
    @Override
    public Voucher updateVoucherQuantity(Long voucherId, Integer newQuantity) {
        Optional<Voucher> voucherOpt = voucherRepo.findById(voucherId);
        if (voucherOpt.isPresent()) {
            Voucher voucher = voucherOpt.get();
            voucher.setQuantity(newQuantity);
            return voucherRepo.save(voucher);
        }
        throw new IllegalArgumentException("Voucher không tồn tại");
    }
}
