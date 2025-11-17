package com.group02.openevent.security.annotation;

import com.group02.openevent.model.enums.Role;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation để yêu cầu role cụ thể để truy cập method hoặc class
 * 
 * @param value Mảng các roles được phép
 * @param requireAll Nếu true, user phải có tất cả roles. Nếu false, chỉ cần 1 trong số roles
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireRole {
    Role[] value();
    boolean requireAll() default false; // true = cần tất cả roles, false = chỉ cần 1 trong số
}

