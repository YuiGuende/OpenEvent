package com.group02.openevent.service.impl;


import com.group02.openevent.exception.WalletException;
import com.group02.openevent.model.payment.TransactionStatus;
import com.group02.openevent.model.payment.TransactionType;
import com.group02.openevent.model.payment.WalletTransaction;
import com.group02.openevent.model.user.Host;
import com.group02.openevent.model.user.HostWallet;
import com.group02.openevent.model.user.User;
import com.group02.openevent.repository.IHostWalletRepository;
import com.group02.openevent.repository.IWalletTransactionRepository;
import com.group02.openevent.service.IHostWalletService;
import com.group02.openevent.service.UserService;
import org.springframework.dao.DataIntegrityViolationException;
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
    private final UserService userService;

    public HostWalletService(IHostWalletRepository walletRepository, IWalletTransactionRepository transactionRepository, UserService userService) {
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
        this.userService = userService;
    }

    // Lấy thông tin ví của Host (KHÔNG tự động tạo mới - ví chỉ được tạo khi host làm KYC và nhập thông tin ngân hàng)
    @Override
    public HostWallet getWalletByHostId(Long hostId) {
        return walletRepository.findByHostId(hostId).orElse(null);
    }

    // Kiểm tra ví đã tồn tại chưa
    @Override
    public boolean walletExists(Long hostId) {
        return walletRepository.findByHostId(hostId).isPresent();
    }

    // Cập nhật KYC name vào ví (nếu ví đã tồn tại)
    @Override
    @Transactional
    public void updateKycName(Long hostId, String kycName) {
        walletRepository.findByHostId(hostId).ifPresent(wallet -> {
            wallet.setKycName(kycName);
            wallet.setKycVerified(true);
            walletRepository.save(wallet);
            logger.info("Updated KYC name for host {}: {}", hostId, kycName);
        });
    }

    // Tạo ví với thông tin ngân hàng và KYC
    @Override
    @Transactional // (Bỏ rollbackFor = WalletException.class)
    public HostWallet createWalletWithBankInfo(Long hostId, String bankAccountNumber, String bankCode,
                                               String accountHolderName, String kycName) throws WalletException, DataIntegrityViolationException {

        // BƯỚC 1: XÓA BỎ VIỆC KIỂM TRA "walletExists" Ở ĐÂY.
        // if (walletExists(hostId)) {
        //     throw new WalletException("Ví đã tồn tại. Không thể tạo mới.");
        // }
        // Cứ để DB xử lý unique constraint, chúng ta sẽ bắt lỗi DataIntegrityViolationException ở controller.
        // Điều này sẽ giải quyết được lỗi StaleObjectStateException.
        User host = userService.getUserByHostId(hostId);
        if (host == null) {
            throw new WalletException("Không tìm thấy Host với ID: " + hostId);
        }
        // Tạo ví mới
        HostWallet wallet = new HostWallet();
        wallet.setHost(host.getHost());
        wallet.setBalance(BigDecimal.ZERO);
        wallet.setAvailableBalance(BigDecimal.ZERO);
        wallet.setBankAccountNumber(bankAccountNumber);
        wallet.setBankCode(bankCode);
        wallet.setAccountHolderName(accountHolderName);
        wallet.setKycName(kycName);
        wallet.setKycVerified(true);
        wallet.setLastUpdated(LocalDateTime.now());

        // Nếu hostId là unique, lệnh save() này sẽ ném DataIntegrityViolationException
        // nếu ví đã tồn tại, điều này an toàn hơn là check-then-act.
        HostWallet savedWallet = walletRepository.save(wallet);
        logger.info("Created wallet with bank info for host {}: bank={}, account={}",
                hostId, bankCode, bankAccountNumber);
        return savedWallet;
    }

    // Trừ tiền tạm thời khỏi ví Host
    @Override
    @Transactional(rollbackFor = WalletException.class)
    public void deductBalance(Long hostId, BigDecimal amount, String referenceId, String description) throws WalletException {
        // 1. Lấy ví Host
        HostWallet wallet = getWalletByHostId(hostId);
        if (wallet == null) {
            throw new WalletException("Ví không tồn tại. Vui lòng tạo ví trước khi rút tiền.");
        }
        
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
        if (wallet == null) {
            logger.warn("Cannot refund to host {}: wallet does not exist", hostId);
            return;
        }
        
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

    // Cộng tiền vào ví Host khi có order thành công
    // LƯU Ý: Method này chỉ cộng tiền vào ví nếu ví đã tồn tại (đã làm KYC và tạo ví)
    // Nếu ví chưa tồn tại, sẽ bỏ qua (host cần tạo ví trước khi nhận thanh toán)
    @Override
    @Transactional
    public void addBalance(Long hostId, BigDecimal amount, String referenceId, String description) {
        // 1. Lấy ví Host
        HostWallet wallet = getWalletByHostId(hostId);
        
        // Nếu ví chưa tồn tại, bỏ qua (host cần tạo ví trước)
        if (wallet == null) {
            logger.warn("Cannot add balance to host {}: wallet does not exist. Host needs to create wallet first.", hostId);
            return;
        }
        
        // 2. Cộng tiền vào cả balance và availableBalance
        wallet.setBalance(wallet.getBalance().add(amount));
        wallet.setAvailableBalance(wallet.getAvailableBalance().add(amount));
        wallet.setLastUpdated(LocalDateTime.now());
        walletRepository.save(wallet);

        // 3. Ghi log giao dịch INCOME với trạng thái COMPLETED
        WalletTransaction transaction = new WalletTransaction();
        transaction.setWallet(wallet);
        transaction.setAmount(amount);
        transaction.setTransactionType(TransactionType.INCOME);
        transaction.setStatus(TransactionStatus.COMPLETED);
        transaction.setReferenceId(referenceId); // Order ID
        transaction.setDescription(description);
        transaction.setCreatedAt(LocalDateTime.now());
        transactionRepository.save(transaction);
        
        logger.info("Added {} to host {} wallet from order {}", amount, hostId, referenceId);
    }
}