package com.group02.openevent.service.impl;

// Giả định đây là Repository
import com.group02.openevent.model.account.Account;
import com.group02.openevent.model.user.CustomUserDetails;
import com.group02.openevent.model.user.User;
import com.group02.openevent.repository.IAccountRepo;
import com.group02.openevent.repository.IUserRepo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collections;
import java.util.List;

/**
 * CustomUserDetailsService chịu trách nhiệm tải thông tin người dùng (UserDetails) 
 * dựa trên tên đăng nhập (email) để Spring Security sử dụng trong quá trình xác thực.
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final IAccountRepo accountRepository;
    private final IUserRepo userRepo;
    Logger logger = LoggerFactory.getLogger(CustomUserDetailsService.class);
    @Autowired
    public CustomUserDetailsService(IAccountRepo accountRepository, IUserRepo userRepo) {
        logger.info("CustomUserDetailsService constructor");
        this.accountRepository = accountRepository;
        this.userRepo = userRepo;
    }

    /**
     * Phương thức cốt lõi được Spring Security gọi khi người dùng cố gắng đăng nhập.
     * @param email Tên đăng nhập (ở đây là email) được gửi từ form.
     * @return Đối tượng CustomUserDetails chứa thông tin người dùng và quyền hạn.
     * @throws UsernameNotFoundException Nếu không tìm thấy người dùng.
     */
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        System.out.println(" loadUserByUsername");
        logger.info("Đang tìm kiếm người dùng với email: " + email); // Thêm logger
        // 1. Tìm kiếm Account (Entity) trong database
        Account account = accountRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Account not found with email: " + email));

        // 2. Lấy User từ Account với eager fetch roles để xác định Role
        User user = userRepo.findByAccountIdWithRoles(account.getAccountId())
                .orElseThrow(() -> new UsernameNotFoundException("User not found for account: " + email));
        
        // 3. Xác định Role từ User entity (customer/host/admin/department)
        com.group02.openevent.model.enums.Role role = user.getRole();
        
        // 4. Tạo danh sách quyền hạn (Authorities) từ Role của User
        // Spring Security's hasRole() method tự động thêm prefix "ROLE_", nên cần thêm prefix này
        List<GrantedAuthority> authorities = Collections.singletonList(
                new SimpleGrantedAuthority("ROLE_" + role.toString()) // Ví dụ: "ROLE_CUSTOMER", "ROLE_DEPARTMENT"
        );

        // 5. Trả về đối tượng CustomUserDetails
        return new CustomUserDetails(
                account.getAccountId(),         // accountId: Dùng để lưu vào Session (ACCOUNT_ID)
                account.getEmail(),      // Username: Dùng để xác thực (tên đăng nhập)
                account.getPasswordHash(),   // Password: Mật khẩu đã mã hóa (BCrypt)
                role.toString(),       // Role: Dùng để lưu vào Session (ACCOUNT_ROLE)
                authorities              // Quyền hạn: Dùng cho Authorization của Spring Security
        );
    }
}
