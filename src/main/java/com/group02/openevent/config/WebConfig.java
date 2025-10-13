package com.group02.openevent.config; // Gói của bạn

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private SessionInterceptor sessionInterceptor;

    // PHẦN BẠN ĐÃ CÓ (GIỮ NGUYÊN) ✅
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
                        "/api/schedules/**"
                );
    }

    // PHẦN CẦN THÊM VÀO ĐỂ SỬA LỖI 404 KHI RELOAD 🚀

}