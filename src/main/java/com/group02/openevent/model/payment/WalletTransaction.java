package com.group02.openevent.model.payment;

import com.group02.openevent.model.user.HostWallet;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
@Getter
@Setter
@Entity
@Table(name = "wallet_transactions")
public class WalletTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Liên kết với Ví của Host
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "host_wallet_id", nullable = false)
    private HostWallet wallet;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", length = 10, nullable = false)
    private TransactionType transactionType;

    @Column(name = "amount", precision = 12, scale = 2, nullable = false)
    private BigDecimal amount; // Luôn là số dương

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 10, nullable = false)
    private TransactionStatus status = TransactionStatus.PENDING;

    // Mã tham chiếu (Mã sự kiện, Mã Payout)
    @Column(name = "reference_id", length = 100, unique = true)
    private String referenceId; 

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

}