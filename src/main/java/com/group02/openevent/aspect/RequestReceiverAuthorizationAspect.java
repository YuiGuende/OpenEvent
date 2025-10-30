package com.group02.openevent.aspect;

import com.group02.openevent.annotation.RequireRequestReceiver;
import com.group02.openevent.model.request.Request;
import com.group02.openevent.repository.IRequestRepo;
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
import java.util.NoSuchElementException;

/**
 * AOP Aspect để kiểm tra nếu người dùng hiện tại là người nhận (receiver) của Request.
 * Chặn các phương thức được đánh dấu @RequireRequestReceiver.
 */
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class RequestReceiverAuthorizationAspect {

    private final IRequestRepo requestRepository;

    @Before("@annotation(requireRequestReceiver)")
    public void checkRequestReceiver(JoinPoint joinPoint, RequireRequestReceiver requireRequestReceiver) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Object[] args = joinPoint.getArgs();

        // Lấy giá trị tham số
        Long requestId = getParameterValue(method, args, requireRequestReceiver.requestIdParamName(), Long.class);
        Long currentUserId = getParameterValue(method, args, requireRequestReceiver.userIdParamName(), Long.class);

        if (requestId == null || currentUserId == null) {
            log.warn("Thiếu các tham số bắt buộc (requestId hoặc currentUserId) để kiểm tra ủy quyền.");
            throw new IllegalArgumentException("Thiếu requestId hoặc currentUserId");
        }

        // Kiểm tra request tồn tại
        Request request = requestRepository.findById(requestId)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy Request với ID: " + requestId));

        // Kiểm tra người nhận
        if (request.getReceiver() == null) {
            log.error("Request {} không có người nhận (receiver).", requestId);
            throw new IllegalStateException("Request không có người nhận.");
        }

        if (!request.getReceiver().getAccountId().equals(currentUserId)) {
            log.warn("Người dùng {} đã cố gắng xử lý Request {} nhưng không phải là người nhận.", currentUserId, requestId);
            throw new AccessDeniedException("Bạn không có quyền thực hiện hành động này.");
        }

        log.info("Kiểm tra ủy quyền thành công cho người dùng {} trên Request {}", currentUserId, requestId);
    }

    /**
     * Phương thức trợ giúp để trích xuất giá trị tham số từ các đối số của phương thức.
     * (Tương tự như trong EventHostAuthorizationAspect) [cite: 266-281]
     */
    private <T> T getParameterValue(Method method, Object[] args, String paramName, Class<T> type) {
        Parameter[] parameters = method.getParameters();
        for (int i = 0; i < parameters.length; i++) {
            Parameter param = parameters[i];

            // Kiểm tra tên tham số
            if (param.getName().equals(paramName)) {
                return type.cast(args[i]);
            }

            // Kiểm tra @RequestParam
            RequestParam requestParam = param.getAnnotation(RequestParam.class);
            if (requestParam != null && requestParam.value().equals(paramName)) {
                return type.cast(args[i]);
            }
        }
        return null;
    }
}