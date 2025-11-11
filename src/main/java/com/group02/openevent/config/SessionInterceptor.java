package com.group02.openevent.config;

import com.group02.openevent.model.session.Session;
import com.group02.openevent.service.SessionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Optional;

@Component
public class SessionInterceptor implements HandlerInterceptor {

    @Autowired
    private SessionService sessionService;

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler) throws Exception {
        // Skip session validation for certain paths
        String requestPath = request.getRequestURI();
        String method = request.getMethod();

        // Log requests to /api/requests for debugging
        if (requestPath.startsWith("/api/requests")) {
            System.out.println("=== SessionInterceptor: " + method + " " + requestPath + " ===");
            System.out.println("Is public path: " + isPublicPath(requestPath));
            if (request.getSession(false) != null) {
                System.out.println("Session exists, ACCOUNT_ID: " + request.getSession(false).getAttribute("ACCOUNT_ID"));
            } else {
                System.out.println("No session found");
            }
        }

        if (isPublicPath(requestPath)) {
            return true;
        }

        // PRIORITY 1: Check HTTP Session first (for web pages)
        // According to CustomAuthenticationSuccessHandler, session stores USER_ID (not ACCOUNT_ID)
        jakarta.servlet.http.HttpSession httpSession = request.getSession(false);
        
        if (httpSession != null) {
            // Session stores USER_ID according to CustomAuthenticationSuccessHandler
            Long userId = (Long) httpSession.getAttribute("USER_ID");
            
            // Also check ACCOUNT_ID for backward compatibility (if some legacy code sets it)
            if (userId == null) {
                userId = (Long) httpSession.getAttribute("ACCOUNT_ID");
            }
            
            if (userId != null) {
                // User is logged in via HTTP session, allow access
                request.setAttribute("currentUserId", userId);
                request.setAttribute("HTTP_SESSION", true);
                // Set USER_ID if not already set (for consistency)
                if (httpSession.getAttribute("USER_ID") == null) {
                    httpSession.setAttribute("USER_ID", userId);
                }
                System.out.println("=== SessionInterceptor: Allowing request with USER_ID: " + userId + " ===");
                return true;
            } else {
                // Log for debugging wallet requests
                if (requestPath.startsWith("/api/wallet")) {
                    System.out.println("=== SessionInterceptor: /api/wallet request ===");
                    System.out.println("Session exists but USER_ID and ACCOUNT_ID are null");
                    System.out.println("Session ID: " + httpSession.getId());
                    java.util.Enumeration<String> attrNames = httpSession.getAttributeNames();
                    System.out.println("Session attributes:");
                    while (attrNames.hasMoreElements()) {
                        String attrName = attrNames.nextElement();
                        System.out.println("  - " + attrName + ": " + httpSession.getAttribute(attrName));
                    }
                }
            }
        } else {
            // Log for debugging wallet requests
            if (requestPath.startsWith("/api/wallet")) {
                System.out.println("=== SessionInterceptor: /api/wallet request ===");
                System.out.println("No HTTP session found");
                System.out.println("Request URI: " + request.getRequestURI());
                System.out.println("Request method: " + request.getMethod());
                System.out.println("Request headers:");
                java.util.Enumeration<String> headerNames = request.getHeaderNames();
                while (headerNames.hasMoreElements()) {
                    String headerName = headerNames.nextElement();
                    System.out.println("  - " + headerName + ": " + request.getHeader(headerName));
                }
                System.out.println("Cookies:");
                if (request.getCookies() != null) {
                    for (jakarta.servlet.http.Cookie cookie : request.getCookies()) {
                        System.out.println("  - " + cookie.getName() + ": " + cookie.getValue());
                    }
                } else {
                    System.out.println("  - No cookies found");
                }
            }
        }

        // PRIORITY 2: Check session token (for API calls)
        String sessionToken = extractSessionToken(request);
        if (sessionToken == null) {
            // For web pages, redirect to login with current URL
            if (request.getRequestURI().startsWith("/event/") ||
                    request.getRequestURI().startsWith("/user/") ||
                    request.getRequestURI().startsWith("/admin/") ||
                    request.getRequestURI().startsWith("/host/")) {

                String currentUrl = request.getRequestURI();
                if (request.getQueryString() != null) {
                    currentUrl += "?" + request.getQueryString();
                }
                response.sendRedirect("/login?redirect=" + java.net.URLEncoder.encode(currentUrl, "UTF-8"));
                return false;
            }

            // For API calls, return JSON error
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write("{\"success\": false, \"error\": \"Session token required\", \"message\": \"Vui lòng đăng nhập lại\"}");
            response.getWriter().flush();
            return false;
        }

        // Validate session
        Optional<Session> sessionOpt = sessionService.validateSession(sessionToken);
        if (sessionOpt.isEmpty()) {
               // Check if this is HTTP session fallback
               if (sessionToken.startsWith("HTTP_SESSION_")) {
                   // Extract USER_ID from HTTP session (session stores USER_ID, not ACCOUNT_ID)
                   Long userId = (Long) request.getSession(false).getAttribute("USER_ID");
                   // Also check ACCOUNT_ID for backward compatibility
                   if (userId == null) {
                       userId = (Long) request.getSession(false).getAttribute("ACCOUNT_ID");
                   }
                   if (userId != null) {
                       // Set request attributes for HTTP session
                       request.setAttribute("currentUserId", userId);
                       request.setAttribute("HTTP_SESSION", true);
                       return true;
                   }
               }

            // Return JSON error for API calls
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write("{\"success\": false, \"error\": \"Invalid or expired session\", \"message\": \"Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại\"}");
            response.getWriter().flush();
            return false;
        }

        Session session = sessionOpt.get();
        request.setAttribute("currentSession", session);
        request.setAttribute("currentAccount", session.getAccount());
        request.setAttribute("currentUserId", session.getAccount().getAccountId());

        return true;
    }

    private boolean isPublicPath(String path) {
        return path.startsWith("/api/auth/") ||
                path.startsWith("/api/sessions/") ||
                path.startsWith("/api/ticket-types/event/") ||
                path.startsWith("/api/event/") ||
                path.startsWith("/api/events/public") ||
                path.startsWith("/api/test/") ||
                path.startsWith("/api/payments/webhook") ||
                path.startsWith("/api/payments/webhook/test") ||
                path.startsWith("/api/payments/webhook/test-data") ||
                path.startsWith("/static/") ||
                path.startsWith("/css/") ||
                path.startsWith("/js/") ||
                path.startsWith("/img/") ||
                path.equals("/") ||
                path.equals("/index.html") ||
                path.startsWith("/login") ||
                path.startsWith("/register") ||
                path.startsWith("/api/events/") ||
                path.startsWith("/api/events/update") ||
                path.startsWith("/api/current-user") ||
                path.startsWith("/api/requests") ||
                path.startsWith("/api/speakers/") ||
                path.startsWith("/api/schedules/") ||
                path.startsWith("/api/event-images/") ||
                path.startsWith("/api/events/update/") ||
                path.startsWith("/api/dashboard/") ||
                path.startsWith("/host/*") ||
                path.startsWith("/fragments/** ") ||
                path.startsWith("/manage/** ") ||
                path.startsWith("/api/payout/") ||
                path.startsWith("/api/ai/") ||
                path.startsWith("/api/ekyc") ||
                path.startsWith("/api/debug/") ||
                path.startsWith("/perform_login");
    }

    private String extractSessionToken(@NonNull HttpServletRequest request) {
        // Try Authorization header first
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }

        // Try request parameter
        String tokenParam = request.getParameter("token");
        if (tokenParam != null && !tokenParam.isEmpty()) {
            return tokenParam;
        }

        // Try session attribute
        Object sessionToken = request.getSession(false) != null ?
                request.getSession(false).getAttribute("SESSION_TOKEN") : null;
        if (sessionToken != null) {
            return sessionToken.toString();
        }

        // If no session token found, check if user is logged in via HTTP session
        // According to CustomAuthenticationSuccessHandler, session stores USER_ID (not ACCOUNT_ID)
        if (request.getSession(false) != null) {
            Long userId = (Long) request.getSession(false).getAttribute("USER_ID");
            // Also check ACCOUNT_ID for backward compatibility
            if (userId == null) {
                userId = (Long) request.getSession(false).getAttribute("ACCOUNT_ID");
            }
            if (userId != null) {
                // User is logged in via HTTP session, allow access
                return "HTTP_SESSION_" + userId;
            }
        }

        return null;
    }
}
