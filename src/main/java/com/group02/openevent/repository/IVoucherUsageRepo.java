package com.group02.openevent.repository;

import com.group02.openevent.model.voucher.VoucherUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IVoucherUsageRepo extends JpaRepository<VoucherUsage, Long> {
    
    List<VoucherUsage> findByVoucherVoucherId(Long voucherId);
    
    List<VoucherUsage> findByOrderOrderId(Long orderId);
    
    @Query("SELECT COUNT(vu) FROM VoucherUsage vu WHERE vu.voucher.voucherId = :voucherId")
    Long countUsageByVoucherId(@Param("voucherId") Long voucherId);
    
    @Query("SELECT vu FROM VoucherUsage vu WHERE vu.voucher.code = :voucherCode")
    List<VoucherUsage> findByVoucherCode(@Param("voucherCode") String voucherCode);
}
