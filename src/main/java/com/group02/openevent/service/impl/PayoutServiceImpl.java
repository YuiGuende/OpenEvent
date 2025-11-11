package com.group02.openevent.service.impl;


import com.group02.openevent.dto.payment.PayoutRequestDto;
import com.group02.openevent.dto.payment.PayoutWebhookData;
import com.group02.openevent.exception.WalletException;
import com.group02.openevent.model.payment.PayoutRequest;
import com.group02.openevent.model.payment.PayoutStatus;
import com.group02.openevent.model.user.Host;
import com.group02.openevent.repository.IHostRepo;
import com.group02.openevent.repository.IPayoutRequestRepository;
import com.group02.openevent.repository.IWalletTransactionRepository;
import com.group02.openevent.service.IHostWalletService;
import com.group02.openevent.service.IPayoutService;
import com.group02.openevent.model.payment.TransactionStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
@Transactional
public class PayoutServiceImpl implements IPayoutService {

    private static final Logger logger = LoggerFactory.getLogger(PayoutServiceImpl.class);
    private final IHostWalletService walletService;
    private final IPayoutRequestRepository payoutRepository;
    private final IHostRepo userRepo;
    private final PayosPayoutClient payosPayoutClient; 
    private final ObjectMapper objectMapper;
    private final IWalletTransactionRepository transactionRepository;

    public PayoutServiceImpl(IHostWalletService walletService, IPayoutRequestRepository payoutRepository, IHostRepo userRepo, PayosPayoutClient payosPayoutClient, ObjectMapper objectMapper, IWalletTransactionRepository transactionRepository) {
        this.walletService = walletService;
        this.payoutRepository = payoutRepository;
        this.userRepo = userRepo;
        this.payosPayoutClient = payosPayoutClient;
        this.objectMapper = objectMapper;
        this.transactionRepository = transactionRepository;
    }

    @Override
    @Transactional(rollbackFor = WalletException.class)
    public PayoutRequest processPayoutRequest(Long hostId, PayoutRequestDto requestDto) throws WalletException {
        // 1. Xác thực Host và tạo Mã giao dịch duy nhất
        Optional<Host> host = userRepo.findById(hostId);
        if (host.isEmpty()) {
            throw new WalletException("Host not found");
        }
        
        // 2. Lấy thông tin ví và kiểm tra ví đã tồn tại chưa
        com.group02.openevent.model.user.HostWallet wallet = walletService.getWalletByHostId(hostId);
        if (wallet == null) {
            throw new WalletException("Ví không tồn tại. Vui lòng tạo ví trước khi rút tiền.");
        }
        
        // 3. Kiểm tra thông tin ngân hàng
        if (wallet.getBankAccountNumber() == null || wallet.getBankAccountNumber().trim().isEmpty()) {
            throw new WalletException("Ví chưa có thông tin ngân hàng. Vui lòng cập nhật thông tin ngân hàng trước khi rút tiền.");
        }
        
        if (wallet.getBankCode() == null || wallet.getBankCode().trim().isEmpty()) {
            throw new WalletException("Ví chưa có mã ngân hàng. Vui lòng cập nhật thông tin ngân hàng trước khi rút tiền.");
        }
        
        String payosOrderCode = "P" + UUID.randomUUID().toString().replace("-", "").substring(0, 20).toUpperCase();
        
        // 4. Trừ tiền Host (Tạm thời trừ số dư khả dụng)
        String description = "Yeu cau rut tien #" + payosOrderCode;
        walletService.deductBalance(hostId, requestDto.getAmount(), payosOrderCode, description);
        
        // 5. Ghi log Yêu cầu Payout vào DB (lấy thông tin ngân hàng từ ví)
        PayoutRequest payout = new PayoutRequest();
        payout.setHost(host.get());
        payout.setAmount(requestDto.getAmount());
        payout.setBankAccountNumber(wallet.getBankAccountNumber()); // Lấy từ ví
        payout.setBankCode(wallet.getBankCode()); // Lấy từ ví
        payout.setPayosOrderCode(payosOrderCode);
        payout.setStatus(PayoutStatus.PENDING);
        payout.setRequestedAt(LocalDateTime.now());
        payoutRepository.save(payout);
        System.out.println("3");
        String payosPayoutId;
        try {
            System.out.println("0");
            // 4. GIẢ VỜ gọi API Payout của PayOS (thực tế PayOS đang có vấn đề)
            // Thay vì gọi thật, chúng ta giả lập thành công và trừ tiền ngay lập tức
            logger.info("Simulating PayOS payout request for order: {}", payosOrderCode);
            
            // Giả lập PayOS transaction ID
            payosPayoutId = "FAKE_PAYOS_" + System.currentTimeMillis();
            
            // Cập nhật trạng thái thành SUCCESS ngay lập tức (vì đã trừ tiền rồi)
            payout.setStatus(PayoutStatus.SUCCESS);
            payout.setPayosTransactionId(payosPayoutId);
            payout.setProcessedAt(LocalDateTime.now());
            payoutRepository.save(payout);
            
            // Cập nhật transaction status từ PENDING sang COMPLETED
            try {
                transactionRepository.findByReferenceId(payosOrderCode).ifPresent(txn -> {
                    txn.setStatus(TransactionStatus.COMPLETED);
                    transactionRepository.save(txn);
                });
            } catch (Exception e) {
                logger.warn("Could not update transaction status: {}", e.getMessage());
            }
            
            System.out.println("1");
            logger.info("Payout processed successfully (fake PayOS). Payout ID: {}", payosPayoutId);
            return payout;
            
        } catch (Exception e) { 
            // 5. Nếu có lỗi xảy ra: Hoàn lại tiền & Cập nhật trạng thái
            logger.error("Failed to process Payout for order {}: {}", payout.getPayosOrderCode(), e.getMessage());
            walletService.refundBalance(hostId, requestDto.getAmount()); 
            
            payout.setStatus(PayoutStatus.FAILURE);
            payoutRepository.save(payout);
            
            // Ném ra WalletException để rollback Transaction
            throw new WalletException("Lỗi khi xử lý yêu cầu rút tiền: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public boolean handlePayoutWebhook(String rawWebhookBody) {
        PayoutWebhookData webhookData;
        
        try {
            // 1. BƯỚC BẢO MẬT: Xác thực Checksum
            if (!payosPayoutClient.verifyWebhookChecksum(rawWebhookBody)) {
                logger.error("SECURITY ALERT: Invalid Checksum received for Payout Webhook. Body: {}", rawWebhookBody);
                return false;
            }
            
            // 2. Parse Webhook Data an toàn sau khi đã xác thực
            webhookData = objectMapper.readValue(rawWebhookBody, PayoutWebhookData.class);

        } catch (Exception e) {
            logger.error("Error parsing Payout Webhook body: {}", e.getMessage());
            return false;
        }

        String orderCode = webhookData.getOrderCode(); // Mã Reference ID
        String status = webhookData.getStatus(); // Trạng thái từ PayOS
        
        // 3. Tìm yêu cầu Payout tương ứng
        PayoutRequest payout = payoutRepository.findByPayosOrderCode(orderCode).orElse(null);
        
        if (payout == null || payout.getStatus() != PayoutStatus.PENDING) {
            logger.warn("Payout Request already processed or not found for orderCode: {}", orderCode);
            return true; 
        }

        // 4. Xử lý trạng thái từ PayOS
        if ("SUCCESS".equalsIgnoreCase(status)) {
            payout.setStatus(PayoutStatus.SUCCESS);
            logger.info("Payout SUCCESS for order: {}", orderCode);

        } else if ("FAILURE".equalsIgnoreCase(status)) {
            payout.setStatus(PayoutStatus.FAILURE);
            
            // 5. Hoàn lại tiền nếu thất bại
            walletService.refundBalance(payout.getHost().getId(), payout.getAmount());
            logger.info("Payout FAILED, amount refunded to host wallet: {}", orderCode);
            
        } else {
            logger.warn("Unhandled Payout status received: {}", status);
            return false;
        }
        
        payout.setProcessedAt(LocalDateTime.now());
        payoutRepository.save(payout);

        return true;
    }
}