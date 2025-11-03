package com.group02.openevent.ai.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Service for AI security measures including input validation and sanitization
 */
@Service
@Slf4j
public class AISecurityService {

    // Patterns for detecting potentially malicious content
    private static final List<Pattern> MALICIOUS_PATTERNS = Arrays.asList(
        // SQL Injection patterns
        Pattern.compile(".*(union|select|insert|update|delete|drop|create|alter|exec|execute).*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*(;|--|/\\*|\\*/).*", Pattern.CASE_INSENSITIVE),
        
        // XSS patterns
        Pattern.compile(".*<script.*>.*</script>.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*javascript:.*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*on\\w+\\s*=.*", Pattern.CASE_INSENSITIVE),
        
        // Command injection patterns
        Pattern.compile(".*(\\||&|;|`|\\$|\\(|\\)).*", Pattern.CASE_INSENSITIVE),
        
        // Path traversal patterns
        Pattern.compile(".*(\\.\\./|\\.\\.\\\\).*", Pattern.CASE_INSENSITIVE),
        
        // Sensitive data patterns
        Pattern.compile(".*(password|passwd|secret|key|token|api_key|private).*", Pattern.CASE_INSENSITIVE)
    );

    // Maximum input length limits
    private static final int MAX_INPUT_LENGTH = 5000;
    private static final int MAX_MESSAGE_LENGTH = 2000;
    private static final int MAX_EVENT_TITLE_LENGTH = 200;
    private static final int MAX_EVENT_DESCRIPTION_LENGTH = 2000;

    // Allowed characters for different input types
    private static final Pattern ALLOWED_EVENT_TITLE_PATTERN = Pattern.compile("^[a-zA-Z0-9\\s\\-_.,!?()àáạảãâầấậẩẫăằắặẳẵèéẹẻẽêềếệểễìíịỉĩòóọỏõôồốộổỗơờớợởỡùúụủũưừứựửữỳýỵỷỹđĐÀÁẠẢÃÂẦẤẬẨẪĂẰẮẶẲẴÈÉẸẺẼÊỀẾỆỂỄÌÍỊỈĨÒÓỌỎÕÔỒỐỘỔỖƠỜỚỢỞỠÙÚỤỦŨƯỪỨỰỬỮỲÝỴỶỸĐ]+$");
    private static final Pattern ALLOWED_EMAIL_PATTERN = Pattern.compile("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");
    private static final Pattern ALLOWED_PHONE_PATTERN = Pattern.compile("^[0-9+\\-\\s()]{9,15}$");

    /**
     * Validate and sanitize user input for AI processing
     */
    public ValidationResult validateInput(String input, InputType type) {
        if (input == null) {
            return ValidationResult.invalid("Input cannot be null");
        }

        // Check for malicious patterns
        for (Pattern pattern : MALICIOUS_PATTERNS) {
            if (pattern.matcher(input).matches()) {
                log.warn("Malicious pattern detected in input: {}", input.substring(0, Math.min(100, input.length())));
                return ValidationResult.invalid("Input contains potentially malicious content");
            }
        }

        // Check length limits
        int maxLength = getMaxLengthForType(type);
        if (input.length() > maxLength) {
            return ValidationResult.invalid("Input exceeds maximum length of " + maxLength + " characters");
        }

        // Type-specific validation
        switch (type) {
            case EVENT_TITLE -> {
                if (!ALLOWED_EVENT_TITLE_PATTERN.matcher(input).matches()) {
                    return ValidationResult.invalid("Event title contains invalid characters");
                }
            }
            case EMAIL -> {
                if (!ALLOWED_EMAIL_PATTERN.matcher(input).matches()) {
                    return ValidationResult.invalid("Invalid email format");
                }
            }
            case PHONE -> {
                if (!ALLOWED_PHONE_PATTERN.matcher(input).matches()) {
                    return ValidationResult.invalid("Invalid phone number format");
                }
            }
            case MESSAGE, EVENT_DESCRIPTION, GENERAL -> {
                // No additional validation for these types
            }
        }

        // Sanitize input
        String sanitized = sanitizeInput(input, type);
        
        return ValidationResult.valid(sanitized);
    }

    /**
     * Sanitize input by removing or escaping potentially dangerous characters
     */
    public String sanitizeInput(String input, InputType type) {
        if (input == null) {
            return null;
        }

        String sanitized = input.trim();

        // Remove or escape HTML/XML tags
        sanitized = sanitized.replaceAll("<[^>]*>", "");
        
        // Escape special characters for different contexts
        switch (type) {
            case EVENT_TITLE, EVENT_DESCRIPTION -> {
                // Remove excessive whitespace
                sanitized = sanitized.replaceAll("\\s+", " ");
            }
            case EMAIL -> {
                // Email is already validated, just trim
                sanitized = sanitized.toLowerCase().trim();
            }
            case PHONE -> {
                // Remove all non-digit characters except + and -
                sanitized = sanitized.replaceAll("[^0-9+\\-]", "");
            }
            case MESSAGE, GENERAL -> {
                // Remove excessive whitespace
                sanitized = sanitized.replaceAll("\\s+", " ");
            }
        }

        return sanitized;
    }

    /**
     * Validate AI response before sending to user
     */
    public ValidationResult validateAIResponse(String response) {
        if (response == null || response.trim().isEmpty()) {
            return ValidationResult.invalid("AI response cannot be empty");
        }

        // Check for malicious content in AI response
        for (Pattern pattern : MALICIOUS_PATTERNS) {
            if (pattern.matcher(response).matches()) {
                log.warn("Malicious pattern detected in AI response");
                return ValidationResult.invalid("AI response contains potentially malicious content");
            }
        }

        // Check length
        if (response.length() > MAX_MESSAGE_LENGTH) {
            return ValidationResult.invalid("AI response exceeds maximum length");
        }

        return ValidationResult.valid(response);
    }

    /**
     * Check if user has permission to access AI features
     */
    public boolean hasAIPermission(String userId, String feature) {
        // Implement permission checking logic here
        // For now, return true for all authenticated users
        return userId != null && !userId.trim().isEmpty();
    }

    /**
     * Log security events
     */
    public void logSecurityEvent(String userId, String event, String details) {
        log.warn("Security Event - User: {} Event: {} Details: {}", userId, event, details);
        // Here you could send to a security monitoring system
    }

    private int getMaxLengthForType(InputType type) {
        return switch (type) {
            case EVENT_TITLE -> MAX_EVENT_TITLE_LENGTH;
            case EVENT_DESCRIPTION -> MAX_EVENT_DESCRIPTION_LENGTH;
            case MESSAGE -> MAX_MESSAGE_LENGTH;
            default -> MAX_INPUT_LENGTH;
        };
    }

    /**
     * Input types for validation
     */
    public enum InputType {
        MESSAGE,
        EVENT_TITLE,
        EVENT_DESCRIPTION,
        EMAIL,
        PHONE,
        GENERAL
    }

    /**
     * Validation result
     */
    public static class ValidationResult {
        private final boolean valid;
        private final String sanitizedInput;
        private final String errorMessage;

        private ValidationResult(boolean valid, String sanitizedInput, String errorMessage) {
            this.valid = valid;
            this.sanitizedInput = sanitizedInput;
            this.errorMessage = errorMessage;
        }

        public static ValidationResult valid(String sanitizedInput) {
            return new ValidationResult(true, sanitizedInput, null);
        }

        public static ValidationResult invalid(String errorMessage) {
            return new ValidationResult(false, null, errorMessage);
        }

        public boolean isValid() {
            return valid;
        }

        public String getSanitizedInput() {
            return sanitizedInput;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }
}
