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
    }
}
