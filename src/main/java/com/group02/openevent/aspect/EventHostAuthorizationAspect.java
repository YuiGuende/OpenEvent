package com.group02.openevent.aspect;


import com.group02.openevent.annotation.RequireEventHost;
import com.group02.openevent.model.event.Event;
import com.group02.openevent.repository.IEventRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestParam;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Optional;

/**
 * AOP Aspect to check if the current user is the host of the event
 * Intercepts methods annotated with @RequireEventHost
 */
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class EventHostAuthorizationAspect {
    
    private final IEventRepo eventRepository;
    
    @Before("@annotation(requireEventHost)")
    public void checkEventHost(JoinPoint joinPoint, RequireEventHost requireEventHost) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Object[] args = joinPoint.getArgs();
        
        // Get parameter names and values
        Long eventId = getParameterValue(method, args, requireEventHost.eventIdParamName(), Long.class);
        Long userId = getParameterValue(method, args, requireEventHost.userIdParamName(), Long.class);
        
        if (eventId == null || userId == null) {
            log.warn("Missing required parameters for authorization check");
            throw new IllegalArgumentException("Missing eventId or userId parameter");
        }
        
        // Check if event exists and user is the host
        Optional<Event> event = eventRepository.findById(eventId);
        
        if (event.isEmpty()) {
            log.warn("Event not found: {}", eventId);
            throw new IllegalArgumentException("Event not found");
        }
        
        // Assuming Event entity has a hostId or userId field
        if (!event.get().getHost().getCustomer().getAccount().getAccountId().equals(userId)) {
            log.warn("User {} attempted to create request for event {} they don't host", userId, eventId);
            throw new AccessDeniedException("Only the event host can create approval requests for this event");
        }
        
        log.info("Authorization check passed for user {} on event {}", userId, eventId);
    }
    
    /**
     * Helper method to extract parameter value from method arguments
     */
    private <T> T getParameterValue(Method method, Object[] args, String paramName, Class<T> type) {
        Parameter[] parameters = method.getParameters();
        
        for (int i = 0; i < parameters.length; i++) {
            Parameter param = parameters[i];
            
            // Check if parameter name matches
            if (param.getName().equals(paramName)) {
                return type.cast(args[i]);
            }
            
            // Check @RequestParam annotation
            RequestParam requestParam = param.getAnnotation(RequestParam.class);
            if (requestParam != null && requestParam.value().equals(paramName)) {
                return type.cast(args[i]);
            }
        }
        
        return null;
    }
}
