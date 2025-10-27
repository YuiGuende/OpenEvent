package com.group02.openevent.ai.service;

import com.group02.openevent.ai.model.Language;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * Service for translating text between different languages
 * Now uses LibreTranslate instead of Google Translate API
 */
@Service
@Slf4j
public class TranslationService {

    private final LibreTranslateService libreTranslateService;

    @Autowired
    public TranslationService(LibreTranslateService libreTranslateService) {
        this.libreTranslateService = libreTranslateService;
    }

    /**
     * Translate text from source language to target language
     */
    public String translate(String text, Language sourceLang, Language targetLang) {
        try {
            return libreTranslateService.translate(text, sourceLang, targetLang);
        } catch (Exception e) {
            log.warn("Translation service unavailable, returning original text: {}", e.getMessage());
            return text; // Return original text if translation fails
        }
    }

    /**
     * Translate text asynchronously
     */
    public CompletableFuture<String> translateAsync(String text, Language sourceLang, Language targetLang) {
        return libreTranslateService.translateAsync(text, sourceLang, targetLang);
    }

    /**
     * Translate AI response to user's preferred language
     */
    public String translateAIResponse(String aiResponse, Language userLanguage) {
        if (aiResponse == null || aiResponse.trim().isEmpty()) {
            return aiResponse;
        }

        // If user language is Vietnamese, return as is
        if (userLanguage == Language.VIETNAMESE) {
            return aiResponse;
        }

        try {
            // Detect the language of AI response (usually Vietnamese or English)
            LanguageDetectionService detector = new LanguageDetectionService();
            Language responseLanguage = detector.detectLanguage(aiResponse);

            if (responseLanguage == userLanguage) {
                return aiResponse;
            }

            return translate(aiResponse, responseLanguage, userLanguage);
        } catch (Exception e) {
            log.warn("Failed to translate AI response, returning original: {}", e.getMessage());
            return aiResponse;
        }
    }

    /**
     * Translate user input to Vietnamese for AI processing
     */
    public String translateUserInput(String userInput, Language userLanguage) {
        if (userInput == null || userInput.trim().isEmpty()) {
            return userInput;
        }

        if (userLanguage == Language.VIETNAMESE) {
            return userInput;
        }

        try {
            return translate(userInput, userLanguage, Language.VIETNAMESE);
        } catch (Exception e) {
            log.warn("Failed to translate user input, returning original: {}", e.getMessage());
            return userInput;
        }
    }

    /**
     * Clear translation cache
     */
    public void clearCache() {
        libreTranslateService.clearCache();
    }

    /**
     * Get cache size
     */
    public int getCacheSize() {
        return libreTranslateService.getCacheSize();
    }

    /**
     * Check if translation service is available
     */
    public boolean isAvailable() {
        return libreTranslateService.isAvailable();
    }

    /**
     * Test translation service connection
     */
    public boolean testConnection() {
        return libreTranslateService.testConnection();
    }

    /**
     * Get supported languages
     */
    public CompletableFuture<java.util.Map<String, String>> getSupportedLanguages() {
        return libreTranslateService.getSupportedLanguages();
    }
}
