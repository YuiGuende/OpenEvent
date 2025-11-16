package com.group02.openevent.aspect;


import com.group02.openevent.annotation.RequireEventHost;
import com.group02.openevent.model.event.Event;
import com.group02.openevent.model.user.Host;
import com.group02.openevent.model.user.User;
import com.group02.openevent.repository.IEventRepo;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Collections;
import java.util.Optional;

/**
 * AOP Aspect to check if the current user is the host of the event
 * Intercepts methods annotated with @RequireEventHost
 */
@Aspect
@Component
@Slf4j
public class EventHostAuthorizationAspect {
    
    private final IEventRepo eventRepository;
    
    // Constructor để log khi aspect được khởi tạo
    public EventHostAuthorizationAspect(IEventRepo eventRepository) {
        this.eventRepository = eventRepository;
        log.info("✅✅✅ EventHostAuthorizationAspect initialized and registered! ✅✅✅");
        log.info("Aspect will intercept methods annotated with @RequireEventHost");
    }
    
    @Before("@annotation(requireEventHost)")
    @Transactional(readOnly = true)
    public void checkEventHost(JoinPoint joinPoint, RequireEventHost requireEventHost) {
        log.info("=== EventHostAuthorizationAspect.checkEventHost CALLED ===");
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Object[] args = joinPoint.getArgs();
        
        log.info("Authorization check started for method: {}", method.getName());
        log.info("Method: {}", method);
        log.info("Args count: {}", args.length);
        
        // Get HttpServletRequest to access session
        HttpServletRequest request = getHttpServletRequest();
        if (request == null) {
            log.warn("Could not get HttpServletRequest for authorization check");
            throw new IllegalStateException("Could not access request");
        }
        
        // Get parameter names and values
        Long eventId = getParameterValue(method, args, requireEventHost.eventIdParamName(), Long.class, request);
        log.info("eventId from parameters: {}", eventId);
        
        // For userId, prioritize session first (because @SessionAttribute may not be resolved when aspect runs)
        Long userId = null;
        if (request != null) {
            userId = getUserIdFromSession(request);
            log.info("userId from session: {}", userId);
        }
        
        // If not found in session, try to get from parameters
        if (userId == null) {
            userId = getParameterValue(method, args, requireEventHost.userIdParamName(), Long.class, request);
            log.info("userId from parameters: {}", userId);
        }
        
        if (userId == null) {
            log.error("Missing required parameters for authorization check - userId: {}", userId);
            log.error("Request parameters: {}", request.getParameterMap().keySet());
            if (request.getSession(false) != null) {
                log.error("Session attributes: {}", Collections.list(request.getSession(false).getAttributeNames()));
                log.error("USER_ID in session: {}", request.getSession(false).getAttribute("USER_ID"));
            } else {
                log.error("No session found");
            }
            throw new IllegalArgumentException("User not authenticated");
        }
        
        // If eventId is null, skip validation (method will handle the error)
        if (eventId == null) {
            log.warn("eventId is null, skipping authorization check (method will handle the error)");
            return;
        }
        
        log.info("Authorization check - eventId: {}, userId: {}", eventId, userId);
        
        // Check if event exists and user is the host
        // Use eager fetch to avoid LazyInitializationException
        Optional<Event> event = eventRepository.findByIdWithHostAccount(eventId);
        if (event.isEmpty()) {
            // Fallback to regular findById if eager fetch method doesn't work
            log.warn("Eager fetch returned empty, trying regular findById");
            event = eventRepository.findById(eventId);
        }
        
        if (event.isEmpty()) {
            log.error("Event not found: {}", eventId);
            throw new IllegalArgumentException("Event not found");
        }
        
        Event eventEntity = event.get();
        log.info("Event found: ID={}, Title={}, Host={}", eventEntity.getId(), eventEntity.getTitle(), 
                eventEntity.getHost() != null ? eventEntity.getHost().getId() : "null");
        
        // Safely extract host account ID with null checks
        Long eventHostId = null;
        try {
            if (eventEntity.getHost() == null) {
                log.error("Event {} has no host assigned", eventId);
                throw new IllegalArgumentException("Event has no host assigned");
            }
            
            Host host = eventEntity.getHost();
            log.info("Host found: hostId={}", host.getId());
            
            // Force initialization of lazy-loaded User relationship
            try {
                // Access user to trigger lazy loading
                User hostUser = host.getUser();
                if (hostUser == null) {
                    log.error("Event {} host has no user assigned", eventId);
                    throw new IllegalArgumentException("Event host has no user assigned");
                }
                
                eventHostId = hostUser.getUserId();
                log.info("Host user found: userId={}, hostId={}", eventHostId, host.getId());
                
                if (eventHostId == null) {
                    log.error("Event {} host user ID is null", eventId);
                    throw new IllegalArgumentException("Event host user ID is null");
                }
            } catch (org.hibernate.LazyInitializationException e) {
                log.error("LazyInitializationException when accessing host.user for event {}: {}", eventId, e.getMessage());
                // Try to reload event with eager fetch
                log.warn("Attempting to reload event with eager fetch for host.user");
                throw new IllegalStateException("Cannot access host user due to lazy loading. Please ensure transaction is active.");
            }
        } catch (IllegalStateException | IllegalArgumentException e) {
            // Re-throw these as-is
            throw e;
        } catch (Exception e) {
            log.error("Error extracting host account ID for event {}: {}", eventId, e.getMessage(), e);
            log.error("Exception type: {}", e.getClass().getName());
            throw new IllegalStateException("Cannot determine event host: " + e.getMessage(), e);
        }
        
        log.info("=== COMPARISON ===");
        log.info("Current userId from session: {} (type: {})", userId, userId != null ? userId.getClass().getName() : "null");
        log.info("Event hostId from database: {} (type: {})", eventHostId, eventHostId != null ? eventHostId.getClass().getName() : "null");
        log.info("Direct comparison (==): {}", userId == eventHostId);
        log.info("Objects.equals(): {}", java.util.Objects.equals(eventHostId, userId));
        log.info("Long.compare(): {}", userId != null && eventHostId != null ? Long.compare(userId, eventHostId) : "N/A");
        
        // Use Objects.equals for safe comparison
        if (!java.util.Objects.equals(eventHostId, userId)) {
            log.error("❌ Authorization FAILED ❌");
            log.error("User {} (from session) attempted to access event {} which belongs to user {} (from database)", 
                    userId, eventId, eventHostId);
            log.error("Event details - ID: {}, Title: '{}'", 
                    eventEntity.getId(), eventEntity.getTitle());
            log.error("Host details - hostId: {}, hostUserId: {}", 
                    eventEntity.getHost() != null ? eventEntity.getHost().getId() : "null", eventHostId);
            throw new AccessDeniedException("Access denied: You can only manage events that you own. Event " + eventId + " belongs to another host.");
        }
        
        log.info("✅ Authorization check PASSED for user {} on event {}", userId, eventId);
    }
    
    /**
     * Get HttpServletRequest from RequestContextHolder
     */
    private HttpServletRequest getHttpServletRequest() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            return attributes != null ? attributes.getRequest() : null;
        } catch (Exception e) {
            log.warn("Could not get HttpServletRequest: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Get user ID from session
     */
    private Long getUserIdFromSession(HttpServletRequest request) {
        try {
            if (request.getSession(false) != null) {
                Object userId = request.getSession(false).getAttribute("USER_ID");
                log.debug("USER_ID from session: {} (type: {})", userId, userId != null ? userId.getClass().getName() : "null");
                
                if (userId instanceof Long) {
                    return (Long) userId;
                } else if (userId != null) {
                    try {
                        Long parsedId = Long.parseLong(userId.toString());
                        log.debug("Parsed USER_ID from session: {}", parsedId);
                        return parsedId;
                    } catch (NumberFormatException e) {
                        log.warn("USER_ID in session is not a valid Long: {}", userId);
                    }
                } else {
                    log.warn("USER_ID is null in session");
                }
            } else {
                log.warn("Session is null when trying to get USER_ID");
            }
        } catch (Exception e) {
            log.error("Error getting userId from session: {}", e.getMessage(), e);
        }
        return null;
    }
    
    /**
     * Helper method to extract parameter value from method arguments
     */
    private <T> T getParameterValue(Method method, Object[] args, String paramName, Class<T> type, HttpServletRequest request) {
        Parameter[] parameters = method.getParameters();
        
        for (int i = 0; i < parameters.length; i++) {
            Parameter param = parameters[i];
            
            // Check if parameter name matches
            if (param.getName().equals(paramName)) {
                Object value = args[i];
                // If value is null but has @SessionAttribute, try to get from session
                if (value == null) {
                    SessionAttribute sessionAttribute = param.getAnnotation(SessionAttribute.class);
                    if (sessionAttribute != null && request != null) {
                        value = getValueFromSession(request, sessionAttribute);
                    }
                }
                // If value is null but has @RequestParam, try to get from request
                if (value == null) {
                    RequestParam requestParam = param.getAnnotation(RequestParam.class);
                    if (requestParam != null && request != null) {
                        String requestParamName = requestParam.value().isEmpty() ? param.getName() : requestParam.value();
                        value = request.getParameter(requestParamName);
                    }
                }
                if (value != null) {
                    return type.cast(value);
                }
            }
            
            // Check @PathVariable annotation
            PathVariable pathVariable = param.getAnnotation(PathVariable.class);
            if (pathVariable != null) {
                String pathVariableValue = pathVariable.value();
                if (pathVariableValue.isEmpty()) {
                    pathVariableValue = param.getName();
                }
                if (pathVariableValue.equals(paramName) || param.getName().equals(paramName)) {
                    Object value = args[i];
                    if (value != null) {
                        return type.cast(value);
                    }
                }
            }
            
            // Check @RequestParam annotation
            RequestParam requestParam = param.getAnnotation(RequestParam.class);
            if (requestParam != null) {
                String requestParamValue = requestParam.value();
                if (requestParamValue.isEmpty()) {
                    requestParamValue = param.getName();
                }
                if (requestParamValue.equals(paramName)) {
                    Object value = args[i];
                    // If value is null, try to get from request
                    if (value == null && request != null) {
                        value = request.getParameter(requestParamValue);
                    }
                    if (value != null) {
                        return type.cast(value);
                    }
                }
            }
            
            // Check @SessionAttribute annotation
            SessionAttribute sessionAttribute = param.getAnnotation(SessionAttribute.class);
            if (sessionAttribute != null && param.getName().equals(paramName)) {
                Object value = args[i];
                // If value is null, try to get from session
                if (value == null && request != null) {
                    value = getValueFromSession(request, sessionAttribute);
                }
                if (value != null) {
                    return type.cast(value);
                }
            }
        }
        
        // If not found in parameters, try to get from request directly
        if (request != null) {
            // Try request parameter first
            String paramValue = request.getParameter(paramName);
            if (paramValue != null && type == Long.class) {
                try {
                    return type.cast(Long.parseLong(paramValue));
                } catch (NumberFormatException e) {
                    log.warn("Could not parse {} as Long: {}", paramValue, e.getMessage());
                }
            }
            
            // Try path variable from URI (for @PathVariable)
            String requestURI = request.getRequestURI();
            if (requestURI != null && paramName.equals("eventId")) {
                // Pattern: /manage/event/{eventId}/...
                java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("/manage/event/(\\d+)");
                java.util.regex.Matcher matcher = pattern.matcher(requestURI);
                if (matcher.find()) {
                    try {
                        return type.cast(Long.parseLong(matcher.group(1)));
                    } catch (NumberFormatException e) {
                        log.warn("Could not parse eventId from URI: {}", matcher.group(1));
                    }
                }
            }
        }
        
        return null;
    }
    
    /**
     * Get value from session based on SessionAttribute annotation
     */
    private Object getValueFromSession(HttpServletRequest request, SessionAttribute sessionAttribute) {
        try {
            if (request.getSession(false) != null) {
                String attributeName = sessionAttribute.value().isEmpty() ? "USER_ID" : sessionAttribute.value();
                return request.getSession(false).getAttribute(attributeName);
            }
        } catch (Exception e) {
            log.warn("Error getting value from session: {}", e.getMessage());
        }
        return null;
    }
}
