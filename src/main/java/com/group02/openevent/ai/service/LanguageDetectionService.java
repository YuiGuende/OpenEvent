package com.group02.openevent.ai.service;

import com.group02.openevent.ai.model.Language;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Service for detecting language from user input
 */
@Service
@Slf4j
public class LanguageDetectionService {

    // Language-specific patterns and keywords
    private static final Map<Language, Pattern[]> LANGUAGE_PATTERNS = new HashMap<>();
    
    static {
        // Vietnamese patterns
        LANGUAGE_PATTERNS.put(Language.VIETNAMESE, new Pattern[]{
            Pattern.compile(".*[àáạảãâầấậẩẫăằắặẳẵèéẹẻẽêềếệểễìíịỉĩòóọỏõôồốộổỗơờớợởỡùúụủũưừứựửữỳýỵỷỹđ].*", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*\\b(tôi|bạn|mình|chúng ta|họ|nó|đây|đó|này|kia)\\b.*", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*\\b(xin chào|tạm biệt|cảm ơn|xin lỗi|không|được|tốt|hay)\\b.*", Pattern.CASE_INSENSITIVE)
        });
        
        // English patterns
        LANGUAGE_PATTERNS.put(Language.ENGLISH, new Pattern[]{
            Pattern.compile(".*\\b(hello|hi|goodbye|thank you|sorry|yes|no|good|great)\\b.*", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*\\b(i|you|we|they|it|this|that|here|there)\\b.*", Pattern.CASE_INSENSITIVE),
            Pattern.compile(".*\\b(please|help|create|make|show|tell)\\b.*", Pattern.CASE_INSENSITIVE)
        });
        
        // Chinese patterns
        LANGUAGE_PATTERNS.put(Language.CHINESE, new Pattern[]{
            Pattern.compile(".*[\\u4e00-\\u9fff].*"),
            Pattern.compile(".*\\b(你好|再见|谢谢|对不起|是的|不是|好的|很好)\\b.*", Pattern.CASE_INSENSITIVE)
        });
        
        // Japanese patterns
        LANGUAGE_PATTERNS.put(Language.JAPANESE, new Pattern[]{
            Pattern.compile(".*[\\u3040-\\u309f\\u30a0-\\u30ff].*"),
            Pattern.compile(".*\\b(こんにちは|さようなら|ありがとう|すみません|はい|いいえ|いい|とても)\\b.*", Pattern.CASE_INSENSITIVE)
        });
        
        // Korean patterns
        LANGUAGE_PATTERNS.put(Language.KOREAN, new Pattern[]{
            Pattern.compile(".*[\\uac00-\\ud7af].*"),
            Pattern.compile(".*\\b(안녕하세요|안녕히가세요|감사합니다|죄송합니다|네|아니요|좋아요|매우)\\b.*", Pattern.CASE_INSENSITIVE)
        });
    }

    /**
     * Detect language from user input text
     */
    public Language detectLanguage(String text) {
        if (text == null || text.trim().isEmpty()) {
            return Language.VIETNAMESE; // Default fallback
        }

        log.debug("Detecting language for text: {}", text.substring(0, Math.min(50, text.length())));

        // Score each language based on pattern matches
        Map<Language, Integer> scores = new HashMap<>();
        
        for (Map.Entry<Language, Pattern[]> entry : LANGUAGE_PATTERNS.entrySet()) {
            Language lang = entry.getKey();
            Pattern[] patterns = entry.getValue();
            int score = 0;
            
            for (Pattern pattern : patterns) {
                if (pattern.matcher(text).matches()) {
                    score++;
                }
            }
            
            scores.put(lang, score);
        }

        // Find language with highest score
        Language detectedLang = Language.VIETNAMESE;
        int maxScore = 0;
        
        for (Map.Entry<Language, Integer> entry : scores.entrySet()) {
            if (entry.getValue() > maxScore) {
                maxScore = entry.getValue();
                detectedLang = entry.getKey();
            }
        }

        // If no patterns matched, use character-based detection
        if (maxScore == 0) {
            detectedLang = Language.detectFromText(text);
        }

        log.debug("Detected language: {} (score: {})", detectedLang.getName(), maxScore);
        return detectedLang;
    }

    /**
     * Get confidence score for language detection
     */
    public double getDetectionConfidence(String text, Language detectedLang) {
        if (text == null || text.trim().isEmpty()) {
            return 0.0;
        }

        Pattern[] patterns = LANGUAGE_PATTERNS.get(detectedLang);
        if (patterns == null) {
            return 0.5; // Medium confidence for character-based detection
        }

        int matches = 0;
        for (Pattern pattern : patterns) {
            if (pattern.matcher(text).matches()) {
                matches++;
            }
        }

        return Math.min(1.0, (double) matches / patterns.length);
    }

    /**
     * Check if text contains mixed languages
     */
    public boolean isMixedLanguage(String text) {
        if (text == null || text.trim().isEmpty()) {
            return false;
        }

        int languageCount = 0;
        for (Language lang : Language.values()) {
            if (LANGUAGE_PATTERNS.containsKey(lang)) {
                Pattern[] patterns = LANGUAGE_PATTERNS.get(lang);
                for (Pattern pattern : patterns) {
                    if (pattern.matcher(text).matches()) {
                        languageCount++;
                        break;
                    }
                }
            }
        }

        return languageCount > 1;
    }
}

