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

        String sessionToken = extractSessionToken(request);
        if (sessionToken == null) {
            // Check if user is logged in via HTTP session
            if (request.getSession(false) != null) {
                Long accountId = (Long) request.getSession(false).getAttribute("ACCOUNT_ID");
                if (accountId != null) {
                    // User is logged in via HTTP session, allow access
                    request.setAttribute("currentUserId", accountId);
                    request.setAttribute("HTTP_SESSION", true);
                    return true;
                }
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
                path.startsWith("/api/current-user") ||
                path.startsWith("/api/events/") ||
                path.startsWith("/api/events/update");
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
