package com.group02.openevent.config;

import jakarta.servlet.http.HttpSession;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import java.security.Principal;
import java.util.Map;

public class UserIdHandshakeHandler extends DefaultHandshakeHandler {
    @Override
    protected Principal determineUser(ServerHttpRequest request, WebSocketHandler wsHandler, Map<String, Object> attributes) {
        if (request instanceof ServletServerHttpRequest sreq) {
            HttpSession session = sreq.getServletRequest().getSession(false);
            if (session != null) {
                Object uid = session.getAttribute("CURRENT_USER_ID");
                if (uid == null) {
                    // Fallback to USER_ID if CURRENT_USER_ID not set
                    Object userId = session.getAttribute("USER_ID");
                    if (userId != null) {
                        uid = userId;
                    }
                }
                if (uid != null) {
                    String name = uid.toString();
                    return () -> name;
                }
            }
        }
        // fallback to default (e.g., Security principal)
        return super.determineUser(request, wsHandler, attributes);
    }
}




