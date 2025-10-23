package com.group02.openevent.model.payment;

import com.group02.openevent.model.user.Host;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
@Getter
@Setter
@Entity
@Table(name = "payout_requests")
public class PayoutRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "host_id", nullable = false)
    private Host host;

    @Column(name = "amount", precision = 12, scale = 2, nullable = false)
    private BigDecimal amount;

    @Column(name = "bank_account_number", length = 50, nullable = false)
    private String bankAccountNumber;

    @Column(name = "bank_code", length = 20, nullable = false)
    private String bankCode;


    // Mã giao dịch duy nhất cho PayOS - QUAN TRỌNG
    @Column(name = "payos_order_code", length = 100, unique = true, nullable = false)
    private String payosOrderCode;

    // ID tham chiếu của PayOS sau khi xử lý (tùy chọn)
    @Column(name = "payos_transaction_id", length = 100)
    private String payosTransactionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 10, nullable = false)
    private PayoutStatus status = PayoutStatus.PENDING;

    @Column(name = "transaction_fee", precision = 12, scale = 2, nullable = false)
    private BigDecimal transactionFee = BigDecimal.ZERO;

    // Checksum/Signature được sử dụng/nhận từ PayOS
    @Column(name = "payos_signature", length = 500)
    private String payosSignature;

    @Column(name = "requested_at", nullable = false)
    private LocalDateTime requestedAt = LocalDateTime.now();

    @Column(name = "processed_at")
    private LocalDateTime processedAt;
}