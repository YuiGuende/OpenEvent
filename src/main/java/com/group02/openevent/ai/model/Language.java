package com.group02.openevent.ai.model;

/**
 * Enum for supported languages in the AI system
 */
public enum Language {
    VIETNAMESE("vi", "Vietnamese", "Tiếng Việt"),
    ENGLISH("en", "English", "English"),
    CHINESE("zh", "Chinese", "中文"),
    JAPANESE("ja", "Japanese", "日本語"),
    KOREAN("ko", "Korean", "한국어"),
    FRENCH("fr", "French", "Français"),
    GERMAN("de", "German", "Deutsch"),
    SPANISH("es", "Spanish", "Español");

    private final String code;
    private final String name;
    private final String nativeName;

    Language(String code, String name, String nativeName) {
        this.code = code;
        this.name = name;
        this.nativeName = nativeName;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getNativeName() {
        return nativeName;
    }

    public static Language fromCode(String code) {
        if (code == null) return null;
        for (Language lang : values()) {
            if (lang.code.equalsIgnoreCase(code)) {
                return lang;
            }
        }
        return null;
    }

    public static Language detectFromText(String text) {
        if (text == null || text.trim().isEmpty()) {
            return VIETNAMESE;
        }

        // Simple language detection based on character patterns
        String lowerText = text.toLowerCase();
        
        // Vietnamese detection
        if (lowerText.matches(".*[àáạảãâầấậẩẫăằắặẳẵèéẹẻẽêềếệểễìíịỉĩòóọỏõôồốộổỗơờớợởỡùúụủũưừứựửữỳýỵỷỹđ].*")) {
            return VIETNAMESE;
        }
        
        // Chinese detection
        if (lowerText.matches(".*[\\u4e00-\\u9fff].*")) {
            return CHINESE;
        }
        
        // Japanese detection
        if (lowerText.matches(".*[\\u3040-\\u309f\\u30a0-\\u30ff].*")) {
            return JAPANESE;
        }
        
        // Korean detection
        if (lowerText.matches(".*[\\uac00-\\ud7af].*")) {
            return KOREAN;
        }
        
        // French detection
        if (lowerText.matches(".*[àâäéèêëïîôöùûüÿç].*")) {
            return FRENCH;
        }
        
        // German detection
        if (lowerText.matches(".*[äöüß].*")) {
            return GERMAN;
        }
        
        // Spanish detection
        if (lowerText.matches(".*[ñáéíóúü].*")) {
            return SPANISH;
        }
        
        // Default to English for Latin script
        return ENGLISH;
    }
}

