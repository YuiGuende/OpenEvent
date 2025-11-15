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
 * CustomOAuth2UserService xử lý OAuth2 user sau khi đăng nhập thành công bằng Google hoặc Facebook
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
        // Gọi parent class để lấy thông tin user từ OAuth provider
        OAuth2User oauth2User = super.loadUser(userRequest);
        
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        Map<String, Object> attributes = new LinkedHashMap<>(oauth2User.getAttributes());
        
        // Xử lý attributes khác nhau cho từng provider
        OAuthUserInfo userInfo = extractUserInfo(registrationId, attributes);
        
        log.info("OAuth2 login - Provider: {}, Email: {}, Name: {}, ProviderId: {}", 
                registrationId, userInfo.getEmail(), userInfo.getName(), userInfo.getProviderId());
        
        // Xử lý email: Facebook có thể không trả về email, dùng provider ID làm fallback
        final String identifier = (userInfo.getEmail() != null && !userInfo.getEmail().isEmpty()) 
                ? userInfo.getEmail() 
                : userInfo.getProviderId() + "@" + registrationId.toLowerCase() + ".oauth";
        
        if (userInfo.getEmail() == null || userInfo.getEmail().isEmpty()) {
            log.warn("Email not provided by {} OAuth, using generated identifier: {}", registrationId, identifier);
        }
        
        // Tìm hoặc tạo Account
        final String finalIdentifier = identifier;
        final String finalProvider = registrationId.toUpperCase();
        final String finalProviderId = userInfo.getProviderId();
        final String finalName = userInfo.getName();
        final String finalPicture = userInfo.getPicture();
        
        Account account = accountRepo.findByEmail(identifier)
                .orElseGet(() -> {
                    // Kiểm tra xem có account nào đã dùng provider ID này chưa
                    return accountRepo.findByOauthProviderAndOauthProviderId(finalProvider, finalProviderId)
                            .orElseGet(() -> createNewOAuthAccount(finalIdentifier, finalName, 
                                    finalPicture, finalProvider, finalProviderId));
                });
        
        // Nếu account đã tồn tại nhưng chưa có OAuth info, cập nhật
        if (account.getOauthProvider() == null) {
            account.setOauthProvider(registrationId.toUpperCase());
            account.setOauthProviderId(userInfo.getProviderId());
            accountRepo.save(account);
        }
        
        // Lấy User từ Account, nếu chưa có thì tạo mới
        User user = userRepo.findByAccount_AccountId(account.getAccountId())
                .orElseGet(() -> {
                    log.warn("User not found for account {}, creating new User", identifier);
                    // Tạo User mới nếu chưa có
                    User newUser = new User();
                    newUser.setAccount(account);
                    newUser.setName(finalName != null ? finalName : account.getEmail());
                    if (finalPicture != null) {
                        newUser.setAvatar(finalPicture);
                    }
                    final User savedUser = userRepo.save(newUser);
                    
                    // Tạo Customer nếu chưa có (default role)
                    customerRepo.findByUser_UserId(savedUser.getUserId())
                            .orElseGet(() -> {
                                Customer newCustomer = new Customer();
                                newCustomer.setUser(savedUser);
                                newCustomer.setPoints(0);
                                return customerRepo.save(newCustomer);
                            });
                    
                    return savedUser;
                });
        
        // Cập nhật thông tin user nếu cần
        boolean userUpdated = false;
        if (userInfo.getName() != null && (user.getName() == null || user.getName().isEmpty())) {
            user.setName(userInfo.getName());
            userUpdated = true;
        }
        if (userInfo.getPicture() != null && (user.getAvatar() == null || user.getAvatar().isEmpty())) {
            user.setAvatar(userInfo.getPicture());
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
        attributes.put("email", identifier);
        httpSession.setAttribute("USER_ID", user.getUserId());
        httpSession.setAttribute("USER_ROLE", role.name());

        // Xác định name attribute key dựa trên provider
        String nameAttributeKey = registrationId.equalsIgnoreCase("google") ? "sub" : "id";
        
        // Trả về DefaultOAuth2User với attributes đã được bổ sung
        return new DefaultOAuth2User(
                Collections.singletonList(authority),
                attributes,
                nameAttributeKey
        ) {
            // Override để trả về email/identifier làm username
            @Override
            public String getName() {
                return identifier;
            }
        };
    }
    
    /**
     * Trích xuất thông tin user từ attributes dựa trên provider
     */
    private OAuthUserInfo extractUserInfo(String registrationId, Map<String, Object> attributes) {
        if ("google".equalsIgnoreCase(registrationId)) {
            // Google attributes: email, name, picture (string)
            String email = (String) attributes.get("email");
            String name = (String) attributes.get("name");
            String picture = (String) attributes.get("picture");
            String providerId = (String) attributes.get("sub");
            return new OAuthUserInfo(email, name, picture, providerId);
        } else if ("facebook".equalsIgnoreCase(registrationId)) {
            // Facebook attributes: id, name, email (có thể null), picture (object)
            String providerId = (String) attributes.get("id");
            String name = (String) attributes.get("name");
            String email = (String) attributes.get("email");
            
            // Facebook picture là object: {data: {url: "..."}}
            String picture = null;
            Object pictureObj = attributes.get("picture");
            if (pictureObj != null && pictureObj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> pictureMap = (Map<String, Object>) pictureObj;
                Object dataObj = pictureMap.get("data");
                if (dataObj != null && dataObj instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> dataMap = (Map<String, Object>) dataObj;
                    picture = (String) dataMap.get("url");
                }
            }
            
            return new OAuthUserInfo(email, name, picture, providerId);
        } else {
            throw new OAuth2AuthenticationException("Unsupported OAuth provider: " + registrationId);
        }
    }
    
    /**
     * Inner class để lưu thông tin user từ OAuth provider
     */
    private static class OAuthUserInfo {
        private final String email;
        private final String name;
        private final String picture;
        private final String providerId;
        
        public OAuthUserInfo(String email, String name, String picture, String providerId) {
            this.email = email;
            this.name = name;
            this.picture = picture;
            this.providerId = providerId;
        }
        
        public String getEmail() { return email; }
        public String getName() { return name; }
        public String getPicture() { return picture; }
        public String getProviderId() { return providerId; }
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

