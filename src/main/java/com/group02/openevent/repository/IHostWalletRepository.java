package com.group02.openevent.repository;


import com.group02.openevent.model.user.HostWallet;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface IHostWalletRepository extends JpaRepository<HostWallet, Long> {
    
    // Tìm ví bằng ID của Host (vì HostWallet ID được ánh xạ từ Host/User ID)
    Optional<HostWallet> findByHostId(Long hostId);
}