package com.group02.openevent.config;

import jakarta.servlet.http.HttpSession;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

/**
 * Custom interceptor to copy HttpSession to WebSocket session attributes
 */
public class HttpSessionWebSocketInterceptor implements HandshakeInterceptor {

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
        if (request instanceof ServletServerHttpRequest servletRequest) {
            HttpSession httpSession = servletRequest.getServletRequest().getSession(false);
            if (httpSession != null) {
                // Copy HttpSession to WebSocket session attributes
                attributes.put("HTTP.SESSION", httpSession);
                attributes.put("HTTP_SESSION", httpSession);
                // Also copy important session attributes individually
                Object userId = httpSession.getAttribute("USER_ID");
                if (userId != null) {
                    attributes.put("USER_ID", userId);
                }
                Object currentUserId = httpSession.getAttribute("CURRENT_USER_ID");
                if (currentUserId != null) {
                    attributes.put("CURRENT_USER_ID", currentUserId);
                }
            }
        }
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        // Nothing to do after handshake
    }
}

