package com.group02.openevent.ai.controller;

import com.group02.openevent.ai.model.Language;
import com.group02.openevent.ai.security.AISecurityService;
import com.group02.openevent.ai.security.RateLimitingService;
import com.group02.openevent.ai.service.LanguageDetectionService;
import com.group02.openevent.ai.service.TranslationService;
import com.group02.openevent.dto.ai.*;
import com.group02.openevent.models.ai.ChatMessage;
import com.group02.openevent.security.SessionUtils;
import com.group02.openevent.security.UserSession;
import com.group02.openevent.services.ai.ChatSessionService;
import io.swagger.v3.oas.annotations.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpSession;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Enhanced AI Controller with multi-language support and security measures
 */
@RestController
@RequestMapping("/api/ai/chat/enhanced")
@CrossOrigin(origins = "*")
@Tag(name = "Enhanced AI Chat", description = "Multi-language AI Chat API with security measures")
@RequiredArgsConstructor
@Slf4j
public class EnhancedAIController {

    private final ChatSessionService chatSessionService;
    private final LanguageDetectionService languageDetectionService;
    private final TranslationService translationService;
    private final AISecurityService securityService;
    private final RateLimitingService rateLimitingService;
    private final SessionUtils sessionUtils;

    @Operation(summary = "List sessions with language support")
    @GetMapping("/sessions")
    public ResponseEntity<List<SessionItem>> sessions(
            @RequestParam(required = false, defaultValue = "vi") String language,
            HttpSession httpSession) {
        
        UserSession us = sessionUtils.requireUser(httpSession);
        String userId = us.getUserId().toString();
        
        // Check rate limit
        if (!rateLimitingService.isAllowed(userId, RateLimitingService.RateLimitType.AI_CHAT)) {
            return ResponseEntity.status(429).build();
        }
        
        List<SessionItem> sessions = chatSessionService.list(us.getUserId());
        return ResponseEntity.ok(sessions);
    }

    @Operation(summary = "Create session with language preference")
    @PostMapping("/sessions")
    public ResponseEntity<NewSessionRes> createSession(
            @RequestBody NewSessionReq req,
            @RequestParam(required = false, defaultValue = "vi") String language,
            HttpSession httpSession) {
        
        UserSession us = sessionUtils.requireUser(httpSession);
        String userId = us.getUserId().toString();
        
        // Check rate limit
        if (!rateLimitingService.isAllowed(userId, RateLimitingService.RateLimitType.AI_CHAT)) {
            return ResponseEntity.status(429).build();
        }
        
        // Validate language
        Language userLanguage = Language.fromCode(language);
        String title = (req.title() == null || req.title().isBlank()) ? "Phiên mới" : req.title();
        
        // Translate title if needed
        if (userLanguage != Language.VIETNAMESE) {
            title = translationService.translate(title, Language.VIETNAMESE, userLanguage);
        }
        
        NewSessionRes result = chatSessionService.create(us.getUserId(), title);
        return ResponseEntity.ok(result);
    }

    @Operation(summary = "Get session history with language support")
    @GetMapping("/history")
    public ResponseEntity<List<ChatMessage>> history(
            @RequestParam String sessionId,
            @RequestParam(required = false, defaultValue = "vi") String language,
            HttpSession httpSession) {
        
        UserSession us = sessionUtils.requireUser(httpSession);
        String userId = us.getUserId().toString();
        
        // Check rate limit
        if (!rateLimitingService.isAllowed(userId, RateLimitingService.RateLimitType.AI_CHAT)) {
            return ResponseEntity.status(429).build();
        }
        
        List<ChatMessage> messages = chatSessionService.history(us.getUserId(), sessionId);
        
        // Translate messages if needed
        Language userLanguage = Language.fromCode(language);
        if (userLanguage != Language.VIETNAMESE) {
            messages = messages.stream()
                    .map(msg -> {
                        if (msg.getIsFromUser()) {
                            // Translate user messages to Vietnamese for processing
                            String translated = translationService.translateUserInput(msg.getMessage(), userLanguage);
                            msg.setMessage(translated);
                        } else {
                            // Translate AI responses to user's language
                            String translated = translationService.translateAIResponse(msg.getMessage(), userLanguage);
                            msg.setMessage(translated);
                        }
                        return msg;
                    })
                    .toList();
        }
        
        return ResponseEntity.ok(messages);
    }

    @Operation(summary = "Enhanced chat with multi-language support")
    @PostMapping
    public ResponseEntity<ChatReply> chat(
            @RequestBody ChatRequest req,
            @RequestParam(required = false, defaultValue = "vi") String language,
            HttpSession httpSession) {
        
        UserSession us = sessionUtils.requireUser(httpSession);
        String userId = us.getUserId().toString();
        
        // Check rate limit
        if (!rateLimitingService.isAllowed(userId, RateLimitingService.RateLimitType.AI_CHAT)) {
            return ResponseEntity.status(429).body(new ChatReply(
                "Rate limit exceeded. Please try again later.", 
                false, 
                LocalDateTime.now()
            ));
        }
        
        // Validate and sanitize input
        AISecurityService.ValidationResult validation = securityService.validateInput(
            req.message(), 
            AISecurityService.InputType.MESSAGE
        );
        
        if (!validation.isValid()) {
            return ResponseEntity.badRequest().body(new ChatReply(
                "❌ " + validation.getErrorMessage(), 
                false, 
                LocalDateTime.now()
            ));
        }
        
        // Detect user language if not specified
        Language userLanguage = Language.fromCode(language);
        if ("auto".equals(language)) {
            userLanguage = languageDetectionService.detectLanguage(req.message());
        }
        
        // Translate user input to Vietnamese for AI processing
        String processedMessage = validation.getSanitizedInput();
        if (userLanguage != Language.VIETNAMESE) {
            processedMessage = translationService.translateUserInput(processedMessage, userLanguage);
        }
        
        // Create chat request with processed message
        ChatRequest processedReq = new ChatRequest(processedMessage, us.getUserId(), req.sessionId());
        
        // Process chat
        ChatReply reply = chatSessionService.chat(processedReq);
        
        // Translate AI response to user's language
        String translatedResponse = reply.message();
        if (userLanguage != Language.VIETNAMESE) {
            translatedResponse = translationService.translateAIResponse(reply.message(), userLanguage);
        }
        
        // Validate AI response
        AISecurityService.ValidationResult responseValidation = securityService.validateAIResponse(translatedResponse);
        if (!responseValidation.isValid()) {
            translatedResponse = "❌ " + responseValidation.getErrorMessage();
        }
        
        return ResponseEntity.ok(new ChatReply(translatedResponse, reply.shouldReload(), reply.timestamp()));
    }

    @Operation(summary = "Detect language of input text")
    @PostMapping("/detect-language")
    public ResponseEntity<Map<String, Object>> detectLanguage(
            @RequestBody Map<String, String> request,
            HttpSession httpSession) {
        
        UserSession us = sessionUtils.requireUser(httpSession);
        String userId = us.getUserId().toString();
        
        // Check rate limit
        if (!rateLimitingService.isAllowed(userId, RateLimitingService.RateLimitType.AI_TRANSLATION)) {
            return ResponseEntity.status(429).build();
        }
        
        String text = request.get("text");
        if (text == null || text.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Text is required"));
        }
        
        // Validate input
        AISecurityService.ValidationResult validation = securityService.validateInput(
            text, 
            AISecurityService.InputType.GENERAL
        );
        
        if (!validation.isValid()) {
            return ResponseEntity.badRequest().body(Map.of("error", validation.getErrorMessage()));
        }
        
        Language detectedLanguage = languageDetectionService.detectLanguage(validation.getSanitizedInput());
        double confidence = languageDetectionService.getDetectionConfidence(validation.getSanitizedInput(), detectedLanguage);
        boolean isMixed = languageDetectionService.isMixedLanguage(validation.getSanitizedInput());
        
        Map<String, Object> result = new HashMap<>();
        result.put("language", detectedLanguage.getCode());
        result.put("languageName", detectedLanguage.getName());
        result.put("nativeName", detectedLanguage.getNativeName());
        result.put("confidence", confidence);
        result.put("isMixedLanguage", isMixed);
        
        return ResponseEntity.ok(result);
    }

    @Operation(summary = "Translate text")
    @PostMapping("/translate")
    public ResponseEntity<Map<String, Object>> translate(
            @RequestBody Map<String, String> request,
            HttpSession httpSession) {
        
        UserSession us = sessionUtils.requireUser(httpSession);
        String userId = us.getUserId().toString();
        
        // Check rate limit
        if (!rateLimitingService.isAllowed(userId, RateLimitingService.RateLimitType.AI_TRANSLATION)) {
            return ResponseEntity.status(429).build();
        }
        
        String text = request.get("text");
        String sourceLang = request.getOrDefault("sourceLang", "auto");
        String targetLang = request.getOrDefault("targetLang", "vi");
        
        if (text == null || text.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Text is required"));
        }
        
        // Validate input
        AISecurityService.ValidationResult validation = securityService.validateInput(
            text, 
            AISecurityService.InputType.GENERAL
        );
        
        if (!validation.isValid()) {
            return ResponseEntity.badRequest().body(Map.of("error", validation.getErrorMessage()));
        }
        
        Language sourceLanguage;
        if ("auto".equals(sourceLang)) {
            sourceLanguage = languageDetectionService.detectLanguage(validation.getSanitizedInput());
        } else {
            sourceLanguage = Language.fromCode(sourceLang);
        }
        
        Language targetLanguage = Language.fromCode(targetLang);
        
        String translatedText = translationService.translate(
            validation.getSanitizedInput(), 
            sourceLanguage, 
            targetLanguage
        );
        
        Map<String, Object> result = new HashMap<>();
        result.put("originalText", text);
        result.put("translatedText", translatedText);
        result.put("sourceLanguage", sourceLanguage.getCode());
        result.put("targetLanguage", targetLanguage.getCode());
        result.put("sourceLanguageName", sourceLanguage.getName());
        result.put("targetLanguageName", targetLanguage.getName());
        
        return ResponseEntity.ok(result);
    }

    @Operation(summary = "Get rate limit information")
    @GetMapping("/rate-limit")
    public ResponseEntity<Map<String, Object>> getRateLimitInfo(HttpSession httpSession) {
        UserSession us = sessionUtils.requireUser(httpSession);
        String userId = us.getUserId().toString();
        
        Map<RateLimitingService.RateLimitType, RateLimitingService.RateLimitInfo> rateLimitInfo = 
            rateLimitingService.getAllRateLimitInfo(userId);
        
        Map<String, Object> result = new HashMap<>();
        rateLimitInfo.forEach((type, info) -> {
            Map<String, Object> typeInfo = new HashMap<>();
            typeInfo.put("maxRequests", info.getMaxRequests());
            typeInfo.put("remainingRequests", info.getRemainingRequests());
            typeInfo.put("resetTime", info.getResetTime());
            typeInfo.put("isLimitExceeded", info.isLimitExceeded());
            result.put(type.name(), typeInfo);
        });
        
        return ResponseEntity.ok(result);
    }

    @Operation(summary = "Get supported languages")
    @GetMapping("/languages")
    public ResponseEntity<List<Map<String, String>>> getSupportedLanguages() {
        List<Map<String, String>> languages = Arrays.stream(Language.values())
                .map(lang -> {
                    Map<String, String> langInfo = new HashMap<>();
                    langInfo.put("code", lang.getCode());
                    langInfo.put("name", lang.getName());
                    langInfo.put("nativeName", lang.getNativeName());
                    return langInfo;
                })
                .toList();
        
        return ResponseEntity.ok(languages);
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("translationAvailable", translationService.isAvailable());
        health.put("translationCacheSize", translationService.getCacheSize());
        health.put("supportedLanguages", Language.values().length);
        return ResponseEntity.ok(health);
    }
}
