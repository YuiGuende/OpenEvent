package com.group02.openevent.controller.payout;


import com.group02.openevent.dto.payment.PayoutRequestDto;
import com.group02.openevent.dto.payment.PayoutResponseDto;
import com.group02.openevent.exception.WalletException;
import com.group02.openevent.model.payment.PayoutRequest;
import com.group02.openevent.service.IPayoutService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/payout")
public class PayoutController {

    private static final Logger logger = LoggerFactory.getLogger(PayoutController.class);
    private final IPayoutService payoutService;

    public PayoutController(IPayoutService payoutService) {
        this.payoutService = payoutService;
    }

    /**
     * Endpoint cho Host yêu cầu rút tiền
     * POST /api/hosts/{hostId}/payouts
     */
    @PostMapping(path = "/{hostId}/request-withdraw")
    public ResponseEntity<?> requestPayout(
            @PathVariable Long hostId,
            @RequestBody PayoutRequestDto requestDto) {

        if (requestDto.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            return ResponseEntity.badRequest().body("Số tiền rút phải lớn hơn 0.");
        }

        try {
            // Gọi Service để xử lý yêu cầu: Kiểm tra số dư, trừ tiền, gọi PayOS
            PayoutRequest payout = payoutService.processPayoutRequest(hostId, requestDto);

            // Convert entity to DTO to avoid circular reference in JSON
            PayoutResponseDto responseDto = PayoutResponseDto.builder()
                    .id(payout.getId())
                    .amount(payout.getAmount())
                    .bankAccountNumber(payout.getBankAccountNumber())
                    .bankCode(payout.getBankCode())
                    .payosOrderCode(payout.getPayosOrderCode())
                    .payosTransactionId(payout.getPayosTransactionId())
                    .status(payout.getStatus())
                    .transactionFee(payout.getTransactionFee())
                    .requestedAt(payout.getRequestedAt())
                    .processedAt(payout.getProcessedAt())
                    .build();

            // Trả về thông tin yêu cầu Payout đã được tạo
            return ResponseEntity.ok(responseDto);

        } catch (WalletException e) {
            // Bắt lỗi nghiệp vụ (ví dụ: số dư không đủ)
            logger.warn("Payout failed for host {}: {}", hostId, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            // Bắt lỗi hệ thống hoặc lỗi PayOS API
            logger.error("System error during payout for host {}: {}", hostId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Lỗi hệ thống khi xử lý yêu cầu rút tiền.");
        }
    }
}