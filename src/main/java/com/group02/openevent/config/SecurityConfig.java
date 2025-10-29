package com.group02.openevent.config;

import com.group02.openevent.service.impl.CustomUserDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order; // <-- Import dòng này
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    @Autowired
    private CustomAuthenticationSuccessHandler successHandler;
    @Autowired
    private CustomUserDetailsService userDetailsService;

    // ============== CHUỖI 1: DÀNH CHO API (STATELESS) ==============
    @Bean
    @Order(1) // Ưu tiên 1 (chạy trước)
    public SecurityFilterChain apiSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                // Chỉ áp dụng chuỗi này cho các request bắt đầu bằng /api/
                .securityMatcher("/api/**")

                // Disable CSRF cho API
                .csrf(csrf -> csrf.disable())

                // Cấu hình ủy quyền cho API
                .authorizeHttpRequests(authz -> authz
                        // Các đường dẫn thanh toán webhook và AI session được phép
                        .requestMatchers("/api/payments/webhook", "/api/payments/webhook/test", "/api/payments/webhook/test-data").permitAll()
                        .requestMatchers("/api/ai/sessions/**").permitAll() // TODO: Như bạn ghi chú
                        .anyRequest().authenticated() // Tất cả các /api/ khác yêu cầu xác thực
                )

                // Dùng STATELESS cho API (không tạo session)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // Bật CORS cho API
                .cors(cors -> cors.configurationSource(corsConfigurationSource()));

        // (Nếu API dùng JWT, bạn sẽ cấu hình thêm ở đây)

        return http.build();
    }

    // ============== CHUỖI 2: DÀNH CHO WEB APP (FORM LOGIN) ==============
    @Bean
    @Order(2) // Ưu tiên 2 (chạy sau chuỗi API)
    public SecurityFilterChain formLoginSecurityFilterChain(HttpSecurity http) throws Exception {
        // Chuỗi này sẽ tự động áp dụng cho MỌI REQUEST KHÁC không khớp với /api/**

        System.out.println("securityFilterChain (Form Login) khởi tạo");
        http
                // Tắt CSRF (Lưu ý: nên bật cho form login, nhưng tôi giữ nguyên theo code của bạn)
                .csrf(csrf -> csrf.disable())

                // Cấu hình Header
                .headers(headers -> headers.cacheControl(cache -> cache.disable()))

                // Cấu hình Authorization
                .authorizeHttpRequests(auth -> auth
                        // Các trang public
                        .requestMatchers("/login", "/register", "/css/**", "/js/**", "/images/**", "/").permitAll()
                        // Tất cả các request khác phải xác thực
                        .anyRequest().authenticated()
                )

                // Cấu hình Form Login
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/perform_login")
                        .successHandler(successHandler)
                        .failureUrl("/login?error")
                        .permitAll()
                )

                // Cấu hình Logout
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .permitAll()
                );

        // (Không cần set session management, nó sẽ mặc định là STATEFUL,
        //  điều này là ĐÚNG cho form login)

        return http.build();
    }


    // ============== CÁC BEAN CHUNG ==============
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
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(
                "http://localhost:3000",
                "http://localhost:8080",
                "http://127.0.0.1:3000",
                "http://127.0.0.1:8080"
        ));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        configuration.setExposedHeaders(Arrays.asList(
                "Authorization",
                "Content-Type",
                "X-Requested-With",
                "accept",
                "Origin",
                "Access-Control-Request-Method",
                "Access-Control-Request-Headers"
        ));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}