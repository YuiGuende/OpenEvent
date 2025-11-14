package com.group02.openevent.model.user;

import com.group02.openevent.model.account.Account;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;
import java.util.Collection;

public class CustomUserDetails extends User {

    private final Long accountId;
    private final String role; // Lưu trữ Role dưới dạng String (hoặc Enum)

    public CustomUserDetails(
        Long accountId,
        String email, 
        String password, 
        String role,
        Collection<? extends GrantedAuthority> authorities) {
        
        // Gọi constructor của lớp cha (Spring Security User)
        super(email, password, authorities);
        this.accountId = accountId;
        this.role = role;
    }

    public Long getAccountId() {
        return accountId;
    }


    public String getRole() {
        return role;
    }
}