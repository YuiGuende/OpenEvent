package com.group02.openevent.ai.controller;

import com.group02.openevent.ai.model.Language;
import com.group02.openevent.ai.service.LibreTranslateService;
import com.group02.openevent.ai.service.TranslationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Controller for translation services
 */
@RestController
@RequestMapping("/api/ai/translation")
@Slf4j
public class TranslationController {

    @Autowired
    private TranslationService translationService;

    @Autowired
    private LibreTranslateService libreTranslateService;

    /**
     * Translate text from one language to another
     */
    @PostMapping("/translate")
    public ResponseEntity<?> translate(
            @RequestParam String text,
            @RequestParam String sourceLang,
            @RequestParam String targetLang) {
        
        try {
            if (!isValidCode(sourceLang) || !isValidCode(targetLang)) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Invalid language code"));
            }
            Language source = Language.fromCode(sourceLang);
            Language target = Language.fromCode(targetLang);

            String translatedText = translationService.translate(text, source, target);
            
            return ResponseEntity.ok(Map.of(
                "originalText", text,
                "translatedText", translatedText,
                "sourceLanguage", source.getName(),
                "targetLanguage", target.getName()
            ));
            
        } catch (Exception e) {
            log.error("Translation failed", e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Translation failed: " + e.getMessage()));
        }
    }

    /**
     * Translate text asynchronously
     */
    @PostMapping("/translate-async")
    public ResponseEntity<Map<String, Object>> translateAsync(
            @RequestParam String text,
            @RequestParam String sourceLang,
            @RequestParam String targetLang) {
        
        try {
            if (!isValidCode(sourceLang) || !isValidCode(targetLang)) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Invalid language code"));
            }
            Language source = Language.fromCode(sourceLang);
            Language target = Language.fromCode(targetLang);

            String translatedText = translationService.translateAsync(text, source, target).get();

            Map<String, Object> response = Map.of(
                    "originalText", text,
                    "translatedText", translatedText,
                    "sourceLanguage", source.getName(),
                    "targetLanguage", target.getName()
            );
            return ResponseEntity.ok(response);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            log.error("Async translation interrupted", ie);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Translation interrupted"));
        } catch (Exception e) {
            log.error("Async translation failed", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Translation failed: " + e.getMessage()));
        }
    }

    private boolean isValidCode(String code) {
        for (Language lang : Language.values()) {
            if (lang.getCode().equalsIgnoreCase(code)) return true;
        }
        return false;
    }

    /**
     * Get supported languages
     */
    @GetMapping("/languages")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> getSupportedLanguages() {
        return libreTranslateService.getSupportedLanguages()
            .thenApply(languages -> ResponseEntity.ok(Map.of(
                "languages", languages,
                "count", languages.size()
            )))
            .exceptionally(throwable -> {
                log.error("Failed to get supported languages", throwable);
                return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to get supported languages: " + throwable.getMessage()));
            });
    }

    /**
     * Test translation service connection
     */
    @GetMapping("/test")
    public ResponseEntity<?> testConnection() {
        try {
            boolean isAvailable = translationService.testConnection();
            
            return ResponseEntity.ok(Map.of(
                "available", isAvailable,
                "service", "LibreTranslate",
                "cacheSize", translationService.getCacheSize()
            ));
            
        } catch (Exception e) {
            log.error("Connection test failed", e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Connection test failed: " + e.getMessage()));
        }
    }

    /**
     * Clear translation cache
     */
    @PostMapping("/clear-cache")
    public ResponseEntity<?> clearCache() {
        try {
            translationService.clearCache();
            
            return ResponseEntity.ok(Map.of(
                "message", "Translation cache cleared successfully",
                "cacheSize", translationService.getCacheSize()
            ));
            
        } catch (Exception e) {
            log.error("Failed to clear cache", e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Failed to clear cache: " + e.getMessage()));
        }
    }

    /**
     * Get translation service status
     */
    @GetMapping("/status")
    public ResponseEntity<?> getStatus() {
        try {
            boolean isAvailable = translationService.isAvailable();
            int cacheSize = translationService.getCacheSize();
            
            return ResponseEntity.ok(Map.of(
                "available", isAvailable,
                "service", "LibreTranslate",
                "cacheSize", cacheSize,
                "status", isAvailable ? "healthy" : "unavailable"
            ));
            
        } catch (Exception e) {
            log.error("Failed to get status", e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Failed to get status: " + e.getMessage()));
        }
    }

    /**
     * Translate AI response to user's preferred language
     */
    @PostMapping("/ai-response")
    public ResponseEntity<?> translateAIResponse(
            @RequestParam String aiResponse,
            @RequestParam String userLanguage) {
        
        try {
            if (!isValidCode(userLanguage)) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Invalid user language code"));
            }
            Language userLang = Language.fromCode(userLanguage);

            String translatedResponse = translationService.translateAIResponse(aiResponse, userLang);
            
            return ResponseEntity.ok(Map.of(
                "originalResponse", aiResponse,
                "translatedResponse", translatedResponse,
                "userLanguage", userLang.getName()
            ));
            
        } catch (Exception e) {
            log.error("AI response translation failed", e);
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "AI response translation failed: " + e.getMessage()));
        }
    }

    /**
     * Translate user input to Vietnamese for AI processing
     */
    @PostMapping("/user-input")
    public ResponseEntity<?> translateUserInput(
            @RequestParam String userInput,
            @RequestParam String userLanguage) {
        
        try {
            if (!isValidCode(userLanguage)) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Invalid user language code"));
            }
            Language userLang = Language.fromCode(userLanguage);

            String translatedInput = translationService.translateUserInput(userInput, userLang);

            return ResponseEntity.ok(Map.of(
                    "originalInput", userInput,
                    "translatedInput", translatedInput,
                    "userLanguage", userLang.getName()
            ));

        } catch (Exception e) {
            log.error("User input translation failed", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "User input translation failed: " + e.getMessage()));
        }
    }
}
