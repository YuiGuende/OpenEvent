package com.group02.openevent.repository;

import com.group02.openevent.model.payment.WalletTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IWalletTransactionRepository extends JpaRepository<WalletTransaction, Long> {

    // Tìm giao dịch ví bằng Mã tham chiếu (ví dụ: PayOS Order Code)
    Optional<WalletTransaction> findByReferenceId(String referenceId);
}