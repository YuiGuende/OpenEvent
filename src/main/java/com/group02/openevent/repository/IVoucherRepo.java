package com.group02.openevent.repository;

import com.group02.openevent.model.voucher.Voucher;
import com.group02.openevent.model.voucher.VoucherStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface IVoucherRepo extends JpaRepository<Voucher, Long> {
    
    Optional<Voucher> findByCode(String code);
    
    List<Voucher> findByStatus(VoucherStatus status);
    
    List<Voucher> findByStatusAndExpiresAtAfter(VoucherStatus status, LocalDateTime now);
    
    List<Voucher> findByStatusAndExpiresAtIsNull(VoucherStatus status);
    
    @Query("SELECT v FROM Voucher v WHERE v.code = :code AND v.status = 'ACTIVE' AND (v.expiresAt IS NULL OR v.expiresAt > :now) AND v.quantity > 0")
    Optional<Voucher> findAvailableVoucherByCode(@Param("code") String code, @Param("now") LocalDateTime now);
    
    @Query("SELECT COUNT(vu) FROM VoucherUsage vu WHERE vu.voucher.voucherId = :voucherId")
    Long countUsageByVoucherId(@Param("voucherId") Long voucherId);
    
    @Query("SELECT v FROM Voucher v WHERE v.status = 'ACTIVE' AND (v.expiresAt IS NULL OR v.expiresAt > :now) AND v.quantity > 0")
    List<Voucher> findAllAvailableVouchers(@Param("now") LocalDateTime now);
}
