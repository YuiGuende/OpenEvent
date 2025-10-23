package com.group02.openevent.service.impl;

// Giả định đây là Repository
import com.group02.openevent.model.account.Account;
import com.group02.openevent.model.user.CustomUserDetails;
import com.group02.openevent.repository.IAccountRepo;
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
    Logger logger = LoggerFactory.getLogger(CustomUserDetailsService.class);
    @Autowired
    public CustomUserDetailsService(IAccountRepo accountRepository) {
        logger.info("CustomUserDetailsService constructor");
        this.accountRepository = accountRepository;
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

        // 2. Tạo danh sách quyền hạn (Authorities) từ Role của Account
        // Giả định Entity Account có phương thức getRole() trả về ROLE_USER, ROLE_ADMIN,...
        List<GrantedAuthority> authorities = Collections.singletonList(
                new SimpleGrantedAuthority(account.getRole().toString()) // Ví dụ: "ROLE_CUSTOMER"
        );

        // 3. Trả về đối tượng CustomUserDetails
        return new CustomUserDetails(
                account.getAccountId(),         // accountId: Dùng để lưu vào Session (ACCOUNT_ID)
                account.getEmail(),      // Username: Dùng để xác thực (tên đăng nhập)
                account.getPasswordHash(),   // Password: Mật khẩu đã mã hóa (BCrypt)
                account.getRole().toString(),       // Role: Dùng để lưu vào Session (ACCOUNT_ROLE)
                authorities              // Quyền hạn: Dùng cho Authorization của Spring Security
        );
    }
}
