package com.group02.openevent.config;

import com.group02.openevent.intercepter.CurrentUriInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private SessionInterceptor sessionInterceptor;
    @Autowired
    private CurrentUriInterceptor currentUriInterceptor;
    @Autowired
    private NetworkGuardInterceptor networkGuardInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(sessionInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/auth/**",
                        "/api/sessions/**",
                        "/api/events/public/**",
                        "/api/current-user",
                        "/api/speakers/**",
                        "/api/schedules/**",
                        "/api/event-images/**"
                );
        registry.addInterceptor(currentUriInterceptor)
                .addPathPatterns("/**")     // áp dụng cho tất cả
                .excludePathPatterns("/api/**");
        
        // NetworkGuardInterceptor: Chỉ áp dụng cho check-in endpoints
        // Note: Endpoint thực tế là /events/api/{eventId}/face-checkin (từ @RequestMapping("/events") + @PostMapping("/api/{eventId}/face-checkin"))
        registry.addInterceptor(networkGuardInterceptor)
                .addPathPatterns(
                        "/api/attendance/**",
                        "/events/api/**/face-checkin",      // Match /events/api/{eventId}/face-checkin
                        "/events/api/**/checkin/**",         // Match các endpoint check-in khác
                        "/events/api/**/checkin-status",     // Match checkin-status
                        "/api/events/**/checkin/**",          // Giữ lại cho các endpoint khác nếu có
                        "/api/events/**/face-checkin",        // Giữ lại cho các endpoint khác nếu có
                        "/api/events/**/checkin-status"       // Giữ lại cho các endpoint khác nếu có
                );
    }
}
