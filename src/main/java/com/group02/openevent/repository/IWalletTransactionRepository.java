package com.group02.openevent.repository;

import com.group02.openevent.model.payment.WalletTransaction;
import com.group02.openevent.model.payment.TransactionType;
import com.group02.openevent.model.user.HostWallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface IWalletTransactionRepository extends JpaRepository<WalletTransaction, Long> {

    // Tìm giao dịch ví bằng Mã tham chiếu (ví dụ: PayOS Order Code)
    Optional<WalletTransaction> findByReferenceId(String referenceId);

    // Tìm tất cả giao dịch theo wallet, sắp xếp theo thời gian giảm dần
    List<WalletTransaction> findByWalletOrderByCreatedAtDesc(HostWallet wallet);

    // Tìm giao dịch theo wallet và transaction type
    @Query("SELECT t FROM WalletTransaction t WHERE t.wallet = :wallet AND " +
           "(:filter = 'all' OR t.transactionType = CAST(:filterType AS com.group02.openevent.model.payment.TransactionType)) " +
           "ORDER BY t.createdAt DESC")
    List<WalletTransaction> findByWalletAndFilter(@Param("wallet") HostWallet wallet, 
                                                   @Param("filter") String filter,
                                                   @Param("filterType") String filterType);
    
    // Helper method để lọc theo type
    default List<WalletTransaction> findByWalletAndFilterType(HostWallet wallet, String filter) {
        if ("all".equals(filter)) {
            return findByWalletOrderByCreatedAtDesc(wallet);
        }
        try {
            TransactionType type = TransactionType.valueOf(filter);
            return findByWalletAndTransactionTypeOrderByCreatedAtDesc(wallet, type);
        } catch (IllegalArgumentException e) {
            return findByWalletOrderByCreatedAtDesc(wallet);
        }
    }
    
    // Tìm giao dịch theo wallet và transaction type
    List<WalletTransaction> findByWalletAndTransactionTypeOrderByCreatedAtDesc(HostWallet wallet, TransactionType transactionType);
}