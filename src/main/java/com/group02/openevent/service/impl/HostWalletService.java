package com.group02.openevent.service.impl;


import com.group02.openevent.exception.WalletException;
import com.group02.openevent.model.payment.TransactionStatus;
import com.group02.openevent.model.payment.TransactionType;
import com.group02.openevent.model.payment.WalletTransaction;
import com.group02.openevent.model.user.HostWallet;
import com.group02.openevent.repository.IHostWalletRepository;
import com.group02.openevent.repository.IWalletTransactionRepository;
import com.group02.openevent.service.IHostWalletService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class HostWalletService implements IHostWalletService {

    private static final Logger logger = LoggerFactory.getLogger(HostWalletService.class);
    
    private final IHostWalletRepository walletRepository;
    private final IWalletTransactionRepository transactionRepository;

    public HostWalletService(IHostWalletRepository walletRepository, IWalletTransactionRepository transactionRepository) {
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
    }

    // Lấy thông tin ví của Host (tạo mới nếu chưa có)
    @Override
    public HostWallet getWalletByHostId(Long hostId) {
        // Tìm ví Host hoặc tạo mới với số dư 0 nếu chưa tồn tại
        return walletRepository.findByHostId(hostId).orElseGet(() -> {
            logger.info("Creating new wallet for host: {}", hostId);
            HostWallet newWallet = new HostWallet();
            newWallet.setHostId(hostId);
            newWallet.setBalance(BigDecimal.ZERO);
            newWallet.setAvailableBalance(BigDecimal.ZERO);
            return walletRepository.save(newWallet);
        });
    }

    // Trừ tiền tạm thời khỏi ví Host
    @Override
    @Transactional(rollbackFor = WalletException.class)
    public void deductBalance(Long hostId, BigDecimal amount, String referenceId, String description) throws WalletException {
        // 1. Lấy ví Host
        HostWallet wallet = getWalletByHostId(hostId);
        
        // 2. Kiểm tra số dư khả dụng (phải lớn hơn hoặc bằng số tiền rút)
        if (wallet.getAvailableBalance().compareTo(amount) < 0) {
            logger.warn("Deduction failed for host {}: Insufficient funds (Available: {}, Requested: {})", 
                        hostId, wallet.getAvailableBalance(), amount);
            throw new WalletException("Số dư khả dụng không đủ để thực hiện giao dịch rút tiền.");
        }

        // 3. Trừ tiền khỏi số dư khả dụng
        wallet.setAvailableBalance(wallet.getAvailableBalance().subtract(amount));
        walletRepository.save(wallet);

        // 4. Ghi log giao dịch Payout với trạng thái PENDING
        WalletTransaction transaction = new WalletTransaction();
        transaction.setWallet(wallet);
        transaction.setAmount(amount);
        transaction.setTransactionType(TransactionType.PAYOUT);
        transaction.setStatus(TransactionStatus.PENDING); // Đang chờ PayOS xác nhận
        transaction.setReferenceId(referenceId); // Mã PayOS Order Code
        transaction.setDescription(description);
        transaction.setCreatedAt(LocalDateTime.now());
        transactionRepository.save(transaction);
        
        logger.info("Temporarily deducted {} from host {} for Payout reference {}", 
                    amount, hostId, referenceId);
    }

    // Cộng tiền lại vào ví Host (Hoàn lại tiền)
    @Override
    @Transactional
    public void refundBalance(Long hostId, BigDecimal amount) {
        // 1. Lấy ví Host
        HostWallet wallet = getWalletByHostId(hostId);
        
        // 2. Cộng tiền hoàn lại vào số dư khả dụng
        wallet.setAvailableBalance(wallet.getAvailableBalance().add(amount));
        walletRepository.save(wallet);
        
        logger.info("Refunded {} back to host {} wallet after Payout failure.", amount, hostId);
        
        // Tùy chọn: Bạn có thể thêm logic ở đây để tìm và cập nhật WalletTransaction từ PENDING sang FAILED
        // IWalletTransactionRepository.findByReferenceId(referenceId).ifPresent(txn -> {
        //     txn.setStatus(TransactionStatus.FAILED);
        //     transactionRepository.save(txn);
        // });
    }
}