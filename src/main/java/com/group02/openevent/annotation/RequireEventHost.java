package com.group02.openevent.annotation;

import java.lang.annotation.*;

/**
 * Annotation to ensure only the event host can perform the action
 * Used with EventHostAuthorizationAspect for authorization checks
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequireEventHost {
    /**
     * Name of the parameter containing the event ID
     */
    String eventIdParamName() default "eventId";
    
    /**
     * Name of the parameter containing the user ID (sender)
     */
    String userIdParamName() default "senderId";
}
