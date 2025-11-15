package com.group02.openevent.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Interceptor để giới hạn vùng mạng cho phép check-in
 * Chỉ cho phép các IP bắt đầu bằng prefix được cấu hình
 */
@Component
@Slf4j
public class NetworkGuardInterceptor implements HandlerInterceptor {

    private final List<String> allowedPrefixes;

    /**
     * Constructor với dependency injection
     * @param allowedPrefixesString Danh sách prefix IP được phép (CSV format)
     */
    public NetworkGuardInterceptor(
            @Value("${checkin.allowed-prefixes:192.168.,10.0.,172.16.}") String allowedPrefixesString) {
        // Parse CSV string thành list, loại bỏ khoảng trắng
        this.allowedPrefixes = Arrays.stream(allowedPrefixesString.split(","))
                .map(String::trim)
                .filter(prefix -> !prefix.isEmpty())
                .collect(Collectors.toList());
        
        log.info("NetworkGuardInterceptor initialized with allowed prefixes: {}", allowedPrefixes);
    }

    @Override
    public boolean preHandle(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull Object handler) throws Exception {
        
        // Lấy IP client
        String clientIp = getClientIp(request);
        
        // Log để debug - dùng INFO level để dễ theo dõi
        log.info("NetworkGuardInterceptor: Checking IP {} for path {}", clientIp, request.getRequestURI());
        log.debug("NetworkGuardInterceptor: Allowed prefixes: {}", allowedPrefixes);
        
        // Kiểm tra xem IP có bắt đầu bằng bất kỳ prefix nào không
        boolean isAllowed = allowedPrefixes.stream()
                .anyMatch(prefix -> clientIp.startsWith(prefix));
        
        if (!isAllowed) {
            log.warn("NetworkGuardInterceptor: Access denied for IP {} to path {}. Allowed prefixes: {}", 
                    clientIp, request.getRequestURI(), allowedPrefixes);
            
            // Trả về HTTP 403 với message
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json;charset=UTF-8");
            response.setCharacterEncoding("UTF-8");
            
            String errorMessage = "{\"success\": false, \"error\": \"Access denied\", " +
                    "\"message\": \"Check-in chỉ được phép trong mạng nội bộ được cấu hình\"}";
            
            response.getWriter().write(errorMessage);
            response.getWriter().flush();
            
            return false;
        }
        
        log.info("NetworkGuardInterceptor: Access allowed for IP {} (matched prefix)", clientIp);
        return true;
    }

    /**
     * Lấy IP client từ request
     * Ưu tiên X-Forwarded-For header (khi có proxy/load balancer)
     * Fallback về request.getRemoteAddr()
     */
    private String getClientIp(HttpServletRequest request) {
        // 1. Kiểm tra X-Forwarded-For header (khi có proxy/load balancer)
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.trim().isEmpty()) {
            // X-Forwarded-For có thể chứa nhiều IP (client, proxy1, proxy2)
            // Lấy IP đầu tiên (client IP thực)
            String clientIp = xForwardedFor.split(",")[0].trim();
            log.info("NetworkGuardInterceptor: Using X-Forwarded-For IP: {} (full header: {})", 
                    clientIp, xForwardedFor);
            return clientIp;
        }
        
        // 2. Kiểm tra X-Real-IP header (một số proxy dùng header này)
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.trim().isEmpty()) {
            log.info("NetworkGuardInterceptor: Using X-Real-IP: {}", xRealIp);
            return xRealIp.trim();
        }
        
        // 3. Fallback về getRemoteAddr()
        String remoteAddr = request.getRemoteAddr();
        log.info("NetworkGuardInterceptor: Using RemoteAddr: {} (no proxy headers found)", remoteAddr);
        
        // Log tất cả headers liên quan để debug
        log.debug("NetworkGuardInterceptor: All request headers:");
        java.util.Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            if (headerName.toLowerCase().contains("forwarded") || 
                headerName.toLowerCase().contains("real") || 
                headerName.toLowerCase().contains("remote")) {
                log.debug("  {}: {}", headerName, request.getHeader(headerName));
            }
        }
        
        return remoteAddr;
    }
}

