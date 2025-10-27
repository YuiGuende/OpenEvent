package com.group02.openevent.ai.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Interceptor for rate limiting AI endpoints
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RateLimitInterceptor implements HandlerInterceptor {

    private final RateLimitingService rateLimitingService;
    private final ObjectMapper objectMapper;

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler) throws Exception {
        String userId = getUserIdFromRequest(request);
        String endpoint = request.getRequestURI();
        
        // Determine rate limit type based on endpoint
        RateLimitingService.RateLimitType rateLimitType = determineRateLimitType(endpoint);
        
        // Check rate limit
        if (!rateLimitingService.isAllowed(userId, rateLimitType)) {
            log.warn("Rate limit exceeded for user: {} endpoint: {}", userId, endpoint);
            
            // Get rate limit info for response
            RateLimitingService.RateLimitInfo rateLimitInfo = rateLimitingService.getRateLimitInfo(userId, rateLimitType);
            
            // Set response headers
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setHeader("X-RateLimit-Limit", String.valueOf(rateLimitInfo.getMaxRequests()));
            response.setHeader("X-RateLimit-Remaining", String.valueOf(rateLimitInfo.getRemainingRequests()));
            response.setHeader("X-RateLimit-Reset", rateLimitInfo.getResetTime().toString());
            response.setHeader("Retry-After", String.valueOf(60)); // Retry after 60 seconds
            
            // Create error response
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Rate limit exceeded");
            errorResponse.put("message", "Too many requests. Please try again later.");
            errorResponse.put("rateLimitInfo", Map.of(
                "maxRequests", rateLimitInfo.getMaxRequests(),
                "remainingRequests", rateLimitInfo.getRemainingRequests(),
                "resetTime", rateLimitInfo.getResetTime()
            ));
            errorResponse.put("timestamp", LocalDateTime.now());
            
            response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
            return false;
        }
        
        // Add rate limit headers to successful responses
        RateLimitingService.RateLimitInfo rateLimitInfo = rateLimitingService.getRateLimitInfo(userId, rateLimitType);
        response.setHeader("X-RateLimit-Limit", String.valueOf(rateLimitInfo.getMaxRequests()));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(rateLimitInfo.getRemainingRequests()));
        response.setHeader("X-RateLimit-Reset", rateLimitInfo.getResetTime().toString());
        
        return true;
    }

    private String getUserIdFromRequest(HttpServletRequest request) {
        // Try to get user ID from session
        Object userIdObj = request.getSession().getAttribute("userId");
        if (userIdObj != null) {
            return userIdObj.toString();
        }
        
        // Try to get from request parameter
        String userId = request.getParameter("userId");
        if (userId != null && !userId.trim().isEmpty()) {
            return userId;
        }
        
        // Try to get from header
        userId = request.getHeader("X-User-ID");
        if (userId != null && !userId.trim().isEmpty()) {
            return userId;
        }
        
        // Fallback to IP address for anonymous users
        String clientIp = getClientIpAddress(request);
        return "anonymous:" + clientIp;
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }

    private RateLimitingService.RateLimitType determineRateLimitType(String endpoint) {
        if (endpoint.contains("/api/ai/chat")) {
            return RateLimitingService.RateLimitType.AI_CHAT;
        } else if (endpoint.contains("/api/ai/translate")) {
            return RateLimitingService.RateLimitType.AI_TRANSLATION;
        } else {
            return RateLimitingService.RateLimitType.GENERAL;
        }
    }
}
