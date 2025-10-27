package com.group02.openevent.ai.config;

import com.group02.openevent.ai.security.RateLimitInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuration for AI security measures
 */
@Configuration
@RequiredArgsConstructor
public class AISecurityConfig implements WebMvcConfigurer {

    private final RateLimitInterceptor rateLimitInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(rateLimitInterceptor)
                .addPathPatterns("/api/ai/**")
                .excludePathPatterns(
                    "/api/ai/health",
                    "/api/ai/languages",
                    "/api/ai/rate-limit"
                );
    }
}

