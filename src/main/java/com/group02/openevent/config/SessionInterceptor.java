package com.group02.openevent.config;

import com.group02.openevent.model.session.Session;
import com.group02.openevent.service.SessionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Optional;

@Component
public class SessionInterceptor implements HandlerInterceptor {

    @Autowired
    private SessionService sessionService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // Skip session validation for certain paths
        String requestPath = request.getRequestURI();
        if (isPublicPath(requestPath)) {
            return true;
        }

        // PRIORITY 1: Check HTTP Session first (for web pages)
        if (request.getSession(false) != null) {
            Long accountId = (Long) request.getSession(false).getAttribute("ACCOUNT_ID");
            if (accountId != null) {
                // User is logged in via HTTP session, allow access
                request.setAttribute("currentUserId", accountId);
                request.setAttribute("HTTP_SESSION", true);
                return true;
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

            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"error\": \"Session token required\"}");
            return false;
        }

        // Validate session
        Optional<Session> sessionOpt = sessionService.validateSession(sessionToken);
        if (sessionOpt.isEmpty()) {
            // Check if this is HTTP session fallback
            if (sessionToken.startsWith("HTTP_SESSION_")) {
                // Extract account ID from HTTP session
                Long accountId = (Long) request.getSession(false).getAttribute("ACCOUNT_ID");
                if (accountId != null) {
                    // Set request attributes for HTTP session
                    request.setAttribute("currentUserId", accountId);
                    request.setAttribute("HTTP_SESSION", true);
                    return true;
                }
            }

            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"error\": \"Invalid or expired session\"}");
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
                path.startsWith("/api/requests")||
                path.startsWith("/api/speakers/") ||
                path.startsWith("/api/schedules/") ||
                path.startsWith("/api/event-images/") ||
                path.startsWith("/api/events/update/") ||
                path.startsWith("/host/*") ||
                path.startsWith("/fragments/** ")||
                path.startsWith("/manage/** ") ||
                path.startsWith("/api/payout/") ||
                path.startsWith("/perform_login");
    }

    private String extractSessionToken(HttpServletRequest request) {
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
        if (request.getSession(false) != null) {
            Long accountId = (Long) request.getSession(false).getAttribute("ACCOUNT_ID");
            if (accountId != null) {
                // User is logged in via HTTP session, allow access
                return "HTTP_SESSION_" + accountId;
            }
        }

        return null;
    }
}
