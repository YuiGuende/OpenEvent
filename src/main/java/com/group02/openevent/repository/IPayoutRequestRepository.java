package com.group02.openevent.repository;

import com.group02.openevent.model.payment.PayoutRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface IPayoutRequestRepository extends JpaRepository<PayoutRequest, Long> {
    
    /**
     * Tìm yêu cầu rút tiền bằng Mã Order Code của PayOS (referenceId bạn gửi đi)
     * Đây là key quan trọng để khớp Payout Request với Webhook.
     */
    Optional<PayoutRequest> findByPayosOrderCode(String payosOrderCode);
}