package com.group02.openevent.controller.payout;


import com.group02.openevent.service.IPayoutService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/webhooks")
@ConditionalOnProperty(name = "payos.payout-client-id", matchIfMissing = false)
public class WebhookController {

    private static final Logger logger = LoggerFactory.getLogger(WebhookController.class);
    private final IPayoutService payoutService;

    public WebhookController(IPayoutService payoutService) {
        this.payoutService = payoutService;
    }

    /**
     * Endpoint nhận Webhook Payout từ PayOS
     * POST /api/webhooks/payos-payout
     * PayOS sẽ gọi endpoint này sau khi xử lý Payout
     */
    @PostMapping("/payos-payout")
    public ResponseEntity<Void> handlePayosPayoutWebhook(@RequestBody String rawWebhookBody) {
        // Ghi log body nguyên thủy để debug (rất quan trọng cho việc xác thực Checksum)
        logger.info("Received PayOS Payout Webhook: {}", rawWebhookBody);
        
        // Gọi Service để xác thực Checksum, Parse và xử lý nghiệp vụ
        boolean success = payoutService.handlePayoutWebhook(rawWebhookBody);

        if (success) {
            // Trả về 200 OK để báo PayOS rằng Webhook đã được nhận và xử lý thành công
            return ResponseEntity.ok().build();
        } else {
            // Trả về 400 hoặc 500 nếu xác thực Checksum thất bại hoặc lỗi nội bộ.
            // Trả về 500 sẽ khiến PayOS cố gắng gửi lại Webhook.
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}