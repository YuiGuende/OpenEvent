package com.group02.openevent.service.impl;

import com.group02.openevent.model.account.Account;
import com.group02.openevent.model.user.Customer;
import com.group02.openevent.model.user.User;
import com.group02.openevent.repository.IAccountRepo;
import com.group02.openevent.repository.ICustomerRepo;
import com.group02.openevent.repository.IUserRepo;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * CustomOAuth2UserService xử lý OAuth2 user sau khi đăng nhập thành công bằng Google
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final IAccountRepo accountRepo;
    private final IUserRepo userRepo;
    private final ICustomerRepo customerRepo;
    private final HttpSession httpSession;
    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        // Gọi parent class để lấy thông tin user từ Google
        OAuth2User oauth2User = super.loadUser(userRequest);
        
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        String providerId = oauth2User.getName(); // Google user ID (sub)
        Map<String, Object> attributes = new LinkedHashMap<>(oauth2User.getAttributes());
        
        // Lấy thông tin từ Google
        String email = (String) attributes.get("email");
        String name = (String) attributes.get("name");
        String picture = (String) attributes.get("picture");
        
        log.info("OAuth2 login - Provider: {}, Email: {}, Name: {}", registrationId, email, name);
        
        if (email == null || email.isEmpty()) {
            throw new OAuth2AuthenticationException("Email not found in OAuth2 user attributes");
        }
        
        // Tìm hoặc tạo Account
        Account account = accountRepo.findByEmail(email)
                .orElseGet(() -> createNewOAuthAccount(email, name, picture, registrationId.toUpperCase(), providerId));
        
        // Nếu account đã tồn tại nhưng chưa có OAuth info, cập nhật
        if (account.getOauthProvider() == null) {
            account.setOauthProvider(registrationId.toUpperCase());
            account.setOauthProviderId(providerId);
            accountRepo.save(account);
        }
        
        // Lấy User từ Account
        User user = userRepo.findByAccount_AccountId(account.getAccountId())
                .orElseThrow(() -> new OAuth2AuthenticationException("User not found for account: " + email));
        
        // Cập nhật thông tin user nếu cần
        boolean userUpdated = false;
        if (name != null && (user.getName() == null || user.getName().isEmpty())) {
            user.setName(name);
            userUpdated = true;
        }
        if (picture != null && (user.getAvatar() == null || user.getAvatar().isEmpty())) {
            user.setAvatar(picture);
            userUpdated = true;
        }
        if (userUpdated) {
            userRepo.save(user);
        }
        
        // Xác định Role
        com.group02.openevent.model.enums.Role role = user.getRole();
        
        // Tạo authorities
        GrantedAuthority authority = new SimpleGrantedAuthority(role.toString());
        
        // Lưu AccountId và Role vào attributes để sử dụng trong AuthenticationSuccessHandler
        attributes.put("accountId", account.getAccountId());
        attributes.put("role", role.toString());
        attributes.put("email", email);
        httpSession.setAttribute("USER_ID", user.getUserId());
        httpSession.setAttribute("USER_ROLE", role.name());

        // Trả về DefaultOAuth2User với attributes đã được bổ sung
        return new DefaultOAuth2User(
                Collections.singletonList(authority),
                attributes,
                "sub" // name attribute key for Google
        ) {
            // Override để trả về email làm username
            @Override
            public String getName() {
                return email;
            }
        };
    }
    
    /**
     * Tạo Account, User và Customer mới cho OAuth user
     */
    private Account createNewOAuthAccount(String email, String name, String picture, 
                                         String provider, String providerId) {
        log.info("Creating new OAuth account for email: {}", email);
        
        // Tạo Account
        Account account = new Account();
        account.setEmail(email);
        account.setPasswordHash(null); // OAuth users don't have password
        account.setOauthProvider(provider);
        account.setOauthProviderId(providerId);
        account = accountRepo.save(account);
        
        // Tạo User
        User user = new User();
        user.setAccount(account);
        user.setName(name != null ? name : "");
        if (picture != null) {
            user.setAvatar(picture);
        }
        user = userRepo.save(user);
        
        // Tạo Customer (default role)
        Customer customer = new Customer();
        customer.setUser(user);
        customer.setPoints(0);
        customerRepo.save(customer);
        
        log.info("Successfully created new OAuth account: {}", email);
        return account;
    }
}

