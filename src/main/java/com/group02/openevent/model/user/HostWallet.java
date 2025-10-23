package com.group02.openevent.model.user;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "host_wallets")
public class HostWallet {

    @Id
    @Column(name = "host_id")
    private Long hostId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "host_id")
    private Host host;

    @Column(name = "balance", precision = 12, scale = 2, nullable = false)
    private BigDecimal balance = BigDecimal.ZERO;

    @Column(name = "available_balance", precision = 12, scale = 2, nullable = false)
    private BigDecimal availableBalance = BigDecimal.ZERO;

    @Column(name = "last_updated")
    private LocalDateTime lastUpdated = LocalDateTime.now();
}