package com.group02.openevent.annotation;

import java.lang.annotation.*;

/**
 * Annotation để đảm bảo chỉ người nhận (receiver) của Request
 * mới có thể thực hiện hành động (ví dụ: approve, reject).
 * Được sử dụng cùng với RequestReceiverAuthorizationAspect để kiểm tra.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequireRequestReceiver {
    /**
     * Tên của tham số chứa Request ID (thường là từ @PathVariable)
     */
    String requestIdParamName() default "requestId";

    /**
     * Tên của tham số chứa User ID (người dùng đang đăng nhập, thường từ @SessionAttribute)
     */
    String userIdParamName() default "currentUserId";
}