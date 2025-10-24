package com.group02.openevent.config;

import com.group02.openevent.service.impl.CustomUserDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {
    @Autowired
    private CustomAuthenticationSuccessHandler successHandler;
    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        System.out.println("DaoAuthenticationProvider khởi tạo");
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(HttpSecurity http) throws Exception {
        AuthenticationManagerBuilder authenticationManagerBuilder =
                http.getSharedObject(AuthenticationManagerBuilder.class);

        authenticationManagerBuilder.authenticationProvider(authenticationProvider());

        return authenticationManagerBuilder.build();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        System.out.println("securityFilterChain khởi tạo");
        http
                // 1. VÔ HIỆU HÓA HOÀN TOÀN CSRF (CHỈ DÀNH CHO MỤC ĐÍCH TEST/DEBUG)
                .csrf(csrf -> csrf.disable())

                // Cấu hình Header để đảm bảo không bị lỗi cache chặn chuyển hướng
                .headers(headers -> headers.cacheControl(cache -> cache.disable()))

                // 2. Cấu hình Authorization
                .authorizeHttpRequests(auth -> auth
                        // Loại bỏ /perform_login khỏi permitAll() để Spring Security xử lý
                        .requestMatchers("/login", "/register", "/css/**", "/js/**", "/images/**", "/", "/api/payments/webhook", "/api/payments/webhook/test", "/api/payments/webhook/test-data").permitAll()
                        .anyRequest().authenticated()
                )

                // 3. Cấu hình Form Login (Quan trọng)
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/perform_login")
                        .successHandler(successHandler) // Handler tùy chỉnh để lưu ACCOUNT_ID
                        .failureUrl("/login?error")
                        .permitAll()
                )

                // 4. Cấu hình Logout
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .permitAll()
                );

        return http.build();
    }
}
