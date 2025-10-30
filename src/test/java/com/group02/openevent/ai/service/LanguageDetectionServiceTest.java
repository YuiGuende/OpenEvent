package com.group02.openevent.ai.service;

import com.group02.openevent.ai.model.Language;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LanguageDetectionServiceTest {

    private LanguageDetectionService service;

    @BeforeEach
    void setup() {
        service = new LanguageDetectionService();
    }

    @Test
    void detectLanguage_blank_defaults_vi() {
        Assertions.assertEquals(Language.VIETNAMESE, service.detectLanguage(""));
        Assertions.assertEquals(Language.VIETNAMESE, service.detectLanguage("   "));
    }

    @Test
    void detectLanguage_patterns_match() {
        Assertions.assertEquals(Language.ENGLISH, service.detectLanguage("hello can you help me"));
        Assertions.assertEquals(Language.VIETNAMESE, service.detectLanguage("xin chào cảm ơn"));
    }

    @Test
    void confidence_and_mixed() {
        var text = "hello xin chào";
        var lang = service.detectLanguage(text);
        double conf = service.getDetectionConfidence(text, lang);
        Assertions.assertTrue(conf >= 0.0 && conf <= 1.0);
        Assertions.assertTrue(service.isMixedLanguage(text));
    }
}


