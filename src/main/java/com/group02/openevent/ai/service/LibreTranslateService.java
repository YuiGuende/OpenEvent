package com.group02.openevent.ai.service;

import com.group02.openevent.ai.model.Language;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for translating text using LibreTranslate API
 * Supports both public instance and self-hosted Docker instance
 */
@Service
@Slf4j
public class LibreTranslateService {

    private final String apiUrl;
    private final String apiKey;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    
    // Cache for translations to avoid repeated API calls
    private final Map<String, String> translationCache = new ConcurrentHashMap<>();
    
    // Fallback URLs in case primary instance is down
    private final String[] fallbackUrls = {
        "https://libretranslate.com",
        "https://translate.argosopentech.com",
        "https://libretranslate.de"
    };

    public LibreTranslateService(@Value("${libretranslate.api.url:https://libretranslate.com}") String apiUrl,
                                @Value("${libretranslate.api.key:}") String apiKey) {
        this.apiUrl = apiUrl.endsWith("/") ? apiUrl.substring(0, apiUrl.length() - 1) : apiUrl;
        this.apiKey = apiKey;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Translate text from source language to target language
     */
    public String translate(String text, Language sourceLang, Language targetLang) {
        if (text == null || text.trim().isEmpty()) {
            return text;
        }

        if (sourceLang == targetLang) {
            return text;
        }

        // Check cache first
        String cacheKey = generateCacheKey(text, sourceLang, targetLang);
        String cachedTranslation = translationCache.get(cacheKey);
        if (cachedTranslation != null) {
            log.debug("Using cached translation for: {}", text.substring(0, Math.min(50, text.length())));
            return cachedTranslation;
        }

        try {
            String translatedText = performTranslation(text, sourceLang, targetLang);
            
            // Cache the result
            translationCache.put(cacheKey, translatedText);
            
            log.debug("Translated '{}' from {} to {}: '{}'", 
                text.substring(0, Math.min(50, text.length())), 
                sourceLang.getName(), 
                targetLang.getName(),
                translatedText.substring(0, Math.min(50, translatedText.length())));
            
            return translatedText;
        } catch (Exception e) {
            log.debug("Translation failed for text (returning original): {} - Error: {}", 
                text.substring(0, Math.min(50, text.length())), e.getMessage());
            return text; // Return original text if translation fails
        }
    }

    /**
     * Translate text asynchronously
     */
    public CompletableFuture<String> translateAsync(String text, Language sourceLang, Language targetLang) {
        return CompletableFuture.supplyAsync(() -> translate(text, sourceLang, targetLang));
    }

    /**
     * Perform actual translation using LibreTranslate API
     */
    private String performTranslation(String text, Language sourceLang, Language targetLang) throws Exception {
        // Try primary URL first, then fallbacks
        String[] urlsToTry = createUrlsToTry();
        
        Exception lastException = null;
        
        for (String url : urlsToTry) {
            try {
                return performTranslationWithUrl(text, sourceLang, targetLang, url);
            } catch (Exception e) {
                log.debug("Translation failed with URL {}: {}", url, e.getMessage());
                lastException = e;
            }
        }
        
        throw new RuntimeException("All LibreTranslate instances failed", lastException);
    }

    /**
     * Perform translation with specific URL
     */
    private String performTranslationWithUrl(String text, Language sourceLang, Language targetLang, String baseUrl) throws Exception {
        String translateUrl = baseUrl + "/translate";
        
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("q", text);
        requestBody.put("source", sourceLang.getCode());
        requestBody.put("target", targetLang.getCode());
        requestBody.put("format", "text");

        // Add API key if available
        if (apiKey != null && !apiKey.trim().isEmpty()) {
            requestBody.put("api_key", apiKey);
        }

        String jsonRequest = objectMapper.writeValueAsString(requestBody);

        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(translateUrl))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(30))
                .POST(HttpRequest.BodyPublishers.ofString(jsonRequest));

        // Add API key as header if available
        if (apiKey != null && !apiKey.trim().isEmpty()) {
            requestBuilder.header("Authorization", "Bearer " + apiKey);
        }

        HttpRequest request = requestBuilder.build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("LibreTranslate API request failed with status " + response.statusCode() + ": " + response.body());
        }

        JsonNode root = objectMapper.readTree(response.body());
        
        // LibreTranslate returns translatedText directly
        if (root.has("translatedText")) {
            return root.get("translatedText").asText();
        }
        
        // Fallback: check for error message
        if (root.has("error")) {
            throw new RuntimeException("LibreTranslate API error: " + root.get("error").asText());
        }

        throw new RuntimeException("Invalid response format from LibreTranslate API: " + response.body());
    }

    /**
     * Create array of URLs to try (primary + fallbacks)
     */
    private String[] createUrlsToTry() {
        String[] urls = new String[fallbackUrls.length + 1];
        urls[0] = apiUrl;
        System.arraycopy(fallbackUrls, 0, urls, 1, fallbackUrls.length);
        return urls;
    }

    /**
     * Get supported languages from LibreTranslate
     */
    public CompletableFuture<Map<String, String>> getSupportedLanguages() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String[] urlsToTry = createUrlsToTry();
                
                for (String url : urlsToTry) {
                    try {
                        return getSupportedLanguagesFromUrl(url);
                    } catch (Exception e) {
                        log.debug("Failed to get languages from {}: {}", url, e.getMessage());
                    }
                }
                
                // Return default languages if all fail
                return getDefaultLanguages();
            } catch (Exception e) {
                log.error("Failed to get supported languages", e);
                return getDefaultLanguages();
            }
        });
    }

    /**
     * Get supported languages from specific URL
     */
    private Map<String, String> getSupportedLanguagesFromUrl(String baseUrl) throws Exception {
        String languagesUrl = baseUrl + "/languages";
        
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(languagesUrl))
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Failed to get languages: " + response.statusCode());
        }

        JsonNode root = objectMapper.readTree(response.body());
        Map<String, String> languages = new HashMap<>();
        
        if (root.isArray()) {
            for (JsonNode lang : root) {
                if (lang.has("code") && lang.has("name")) {
                    languages.put(lang.get("code").asText(), lang.get("name").asText());
                }
            }
        }
        
        return languages;
    }

    /**
     * Get default supported languages
     */
    private Map<String, String> getDefaultLanguages() {
        Map<String, String> languages = new HashMap<>();
        languages.put("vi", "Vietnamese");
        languages.put("en", "English");
        languages.put("zh", "Chinese");
        languages.put("ja", "Japanese");
        languages.put("ko", "Korean");
        languages.put("fr", "French");
        languages.put("de", "German");
        languages.put("es", "Spanish");
        languages.put("it", "Italian");
        languages.put("pt", "Portuguese");
        languages.put("ru", "Russian");
        languages.put("ar", "Arabic");
        languages.put("th", "Thai");
        languages.put("id", "Indonesian");
        languages.put("ms", "Malay");
        return languages;
    }

    /**
     * Generate cache key for translation
     */
    private String generateCacheKey(String text, Language sourceLang, Language targetLang) {
        return sourceLang.getCode() + ":" + targetLang.getCode() + ":" + text.hashCode();
    }

    /**
     * Clear translation cache
     */
    public void clearCache() {
        translationCache.clear();
        log.info("Translation cache cleared");
    }

    /**
     * Get cache size
     */
    public int getCacheSize() {
        return translationCache.size();
    }

    /**
     * Check if translation service is available
     */
    public boolean isAvailable() {
        try {
            // Try to get supported languages to check availability
            getSupportedLanguages().get();
            return true;
        } catch (Exception e) {
            log.warn("LibreTranslate service not available: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Test translation service with a simple text
     */
    public boolean testConnection() {
        try {
            String testText = "Hello";
            String result = translate(testText, Language.ENGLISH, Language.VIETNAMESE);
            return result != null && !result.equals(testText);
        } catch (Exception e) {
            log.error("Translation service test failed", e);
            return false;
        }
    }
}

