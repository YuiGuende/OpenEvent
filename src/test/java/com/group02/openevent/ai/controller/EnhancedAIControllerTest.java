package com.group02.openevent.ai.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.group02.openevent.ai.model.Language;
import com.group02.openevent.ai.security.AISecurityService;
import com.group02.openevent.ai.security.RateLimitingService;
import com.group02.openevent.ai.service.LanguageDetectionService;
import com.group02.openevent.ai.service.TranslationService;
import com.group02.openevent.dto.ai.ChatReply;
import com.group02.openevent.dto.ai.ChatRequest;
import com.group02.openevent.services.ai.ChatSessionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@ActiveProfiles("test")
class EnhancedAIControllerTest {

	@Mock private ChatSessionService chatSessionService;
	@Mock private LanguageDetectionService languageDetectionService;
	@Mock private TranslationService translationService;
	@Mock private AISecurityService securityService;
	@Mock private RateLimitingService rateLimitingService;

	@InjectMocks private EnhancedAIController controller;

	private MockMvc mockMvc;
	private final ObjectMapper om = new ObjectMapper();
	private MockHttpSession session;

	@BeforeEach
	void setup() {
		MockitoAnnotations.openMocks(this);
		mockMvc = org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup(controller).build();
		session = new MockHttpSession();
		session.setAttribute("USER_ID", 123L);
		session.setAttribute("ACCOUNT_ROLE", "USER");
	}

	@ParameterizedTest(name = "lang={0}, detected={1}")
	@CsvSource({"auto,ENGLISH","auto,VIETNAMESE","en,ENGLISH","vi,VIETNAMESE"})
	void chat_translations(String lang, Language detected) throws Exception {
		when(rateLimitingService.isAllowed(anyString(), any())).thenReturn(true);
		when(securityService.validateInput(anyString(), any()))
				.thenReturn(AISecurityService.ValidationResult.valid("hi"));
		if ("auto".equals(lang)) when(languageDetectionService.detectLanguage(anyString())).thenReturn(detected);
		when(translationService.translateUserInput(anyString(), any(Language.class))).thenReturn("xin chao");
		when(chatSessionService.chat(any())).thenReturn(new ChatReply("chao ban", false, LocalDateTime.now()));
		when(translationService.translateAIResponse(anyString(), any(Language.class))).thenReturn("hello you");
		when(securityService.validateAIResponse(anyString()))
				.thenReturn(AISecurityService.ValidationResult.valid("hello you"));

		var req = new ChatRequest("hi", 123L, "S1");

		Language target = "auto".equals(lang)
				? detected
				: ("en".equals(lang) ? Language.ENGLISH : Language.VIETNAMESE);
		String expectedMessage = (target == Language.ENGLISH) ? "hello you" : "chao ban";

		mockMvc.perform(post("/api/ai/chat/enhanced?language=" + lang)
				.contentType(MediaType.APPLICATION_JSON)
				.session(session)
				.content(om.writeValueAsString(req)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.message").value(expectedMessage));
	}

	@ParameterizedTest(name = "allowed={0}, status={1}")
	@CsvSource({"false,429","true,200"})
	void chat_rateLimit(String allowed, int expectedStatus) throws Exception {
		when(rateLimitingService.isAllowed(anyString(), any()))
				.thenReturn(Boolean.parseBoolean(allowed));
		if (expectedStatus == 200) {
			when(securityService.validateInput(anyString(), any()))
					.thenReturn(AISecurityService.ValidationResult.valid("hi"));
			when(chatSessionService.chat(any())).thenReturn(new ChatReply("ok", false, LocalDateTime.now()));
			when(securityService.validateAIResponse(anyString()))
					.thenReturn(AISecurityService.ValidationResult.valid("ok"));
		}
		var req = new ChatRequest("hi", 123L, "S1");

		mockMvc.perform(post("/api/ai/chat/enhanced")
				.contentType(MediaType.APPLICATION_JSON)
				.session(session)
				.content(om.writeValueAsString(req)))
				.andExpect(status().is(expectedStatus));
	}

	@ParameterizedTest(name = "aiResponseValid={0}")
	@ValueSource(booleans = {true, false})
	void chat_aiResponseValidation(boolean valid) throws Exception {
		when(rateLimitingService.isAllowed(anyString(), any())).thenReturn(true);
		when(securityService.validateInput(anyString(), any()))
				.thenReturn(AISecurityService.ValidationResult.valid("hi"));
		when(chatSessionService.chat(any()))
				.thenReturn(new ChatReply("raw", false, LocalDateTime.now()));
		when(securityService.validateAIResponse(anyString()))
				.thenReturn(valid ? AISecurityService.ValidationResult.valid("raw")
					: AISecurityService.ValidationResult.invalid("blocked"));

		var req = new ChatRequest("hi", 123L, "S1");

		var res = mockMvc.perform(post("/api/ai/chat/enhanced")
				.contentType(MediaType.APPLICATION_JSON)
				.session(session)
				.content(om.writeValueAsString(req)))
				.andExpect(status().isOk())
				.andReturn();

		String body = res.getResponse().getContentAsString();
		if (valid) org.junit.jupiter.api.Assertions.assertTrue(body.contains("raw"));
		else org.junit.jupiter.api.Assertions.assertTrue(body.contains("blocked"));
	}

	@ParameterizedTest(name = "sessions rateLimit allowed={0} -> status={1}")
	@CsvSource({"false,429","true,200"})
	void sessions_rateLimit(String allowed, int expected) throws Exception {
		when(rateLimitingService.isAllowed(anyString(), eq(RateLimitingService.RateLimitType.AI_CHAT)))
				.thenReturn(Boolean.parseBoolean(allowed));
		mockMvc.perform(get("/api/ai/chat/enhanced/sessions").session(session))
				.andExpect(status().is(expected));
	}

	@ParameterizedTest(name = "createSession lang={0}")
	@CsvSource({"vi","en"})
	void createSession_languageFlow(String lang) throws Exception {
		when(rateLimitingService.isAllowed(anyString(), eq(RateLimitingService.RateLimitType.AI_CHAT)))
				.thenReturn(true);
		when(translationService.translate(anyString(), any(Language.class), any(Language.class)))
				.thenReturn("Translated Title");
		String body = "{\"title\":\"\"}";
		mockMvc.perform(post("/api/ai/chat/enhanced/sessions?language=" + lang)
				.contentType(MediaType.APPLICATION_JSON)
				.session(session)
				.content(body))
				.andExpect(status().isOk());
	}

	@org.junit.jupiter.api.Test
	void createSession_rateLimitDenied_429() throws Exception {
		when(rateLimitingService.isAllowed(anyString(), eq(RateLimitingService.RateLimitType.AI_CHAT)))
				.thenReturn(false);
		mockMvc.perform(post("/api/ai/chat/enhanced/sessions")
				.contentType(MediaType.APPLICATION_JSON)
				.session(session)
				.content("{\"title\":\"\"}"))
				.andExpect(status().isTooManyRequests());
	}

	@ParameterizedTest(name = "history lang={0}")
	@CsvSource({"vi","en"})
	void history_languageFlow(String lang) throws Exception {
		when(rateLimitingService.isAllowed(anyString(), eq(RateLimitingService.RateLimitType.AI_CHAT)))
				.thenReturn(true);
		when(translationService.translateUserInput(anyString(), any(Language.class))).thenReturn("xin chao");
		when(translationService.translateAIResponse(anyString(), any(Language.class))).thenReturn("hello");
		mockMvc.perform(get("/api/ai/chat/enhanced/history")
				.param("sessionId","S1")
				.param("language", lang)
				.session(session))
				.andExpect(status().isOk());
	}

	@org.junit.jupiter.api.Test
	void history_rateLimitDenied_429() throws Exception {
		when(rateLimitingService.isAllowed(anyString(), eq(RateLimitingService.RateLimitType.AI_CHAT)))
				.thenReturn(false);
		mockMvc.perform(get("/api/ai/chat/enhanced/history")
				.param("sessionId","S1")
				.session(session))
				.andExpect(status().isTooManyRequests());
	}

	@ParameterizedTest
	@ValueSource(strings = {"", "  "})
	void detectLanguage_blank_400(String text) throws Exception {
        when(rateLimitingService.isAllowed(anyString(),
                eq(RateLimitingService.RateLimitType.AI_TRANSLATION)))
                .thenReturn(true);

		mockMvc.perform(post("/api/ai/chat/enhanced/detect-language")
				.contentType(MediaType.APPLICATION_JSON)
				.session(session)
				.content("{\"text\":\"" + text + "\"}"))
				.andExpect(status().isBadRequest());
	}

	@org.junit.jupiter.api.Test
	void detectLanguage_rateLimitDenied_429() throws Exception {
		when(rateLimitingService.isAllowed(anyString(), eq(RateLimitingService.RateLimitType.AI_TRANSLATION)))
				.thenReturn(false);
		mockMvc.perform(post("/api/ai/chat/enhanced/detect-language")
				.contentType(MediaType.APPLICATION_JSON)
				.session(session)
				.content("{\"text\":\"hello\"}"))
				.andExpect(status().isTooManyRequests());
	}

	@ParameterizedTest(name = "enhanced translate source={0}, target={1}")
	@CsvSource({"auto,vi","en,vi"})
	void enhancedTranslate_ok(String source, String target) throws Exception {
		when(rateLimitingService.isAllowed(anyString(), eq(RateLimitingService.RateLimitType.AI_TRANSLATION)))
				.thenReturn(true);
		when(securityService.validateInput(anyString(), any()))
				.thenReturn(AISecurityService.ValidationResult.valid("hello"));
		when(languageDetectionService.detectLanguage(anyString())).thenReturn(Language.ENGLISH);
		when(translationService.translate(anyString(), any(Language.class), any(Language.class)))
				.thenReturn("xin chào");
		String body = "{\"text\":\"hello\",\"sourceLang\":\"" + source + "\",\"targetLang\":\"" + target + "\"}";
		mockMvc.perform(post("/api/ai/chat/enhanced/translate")
				.contentType(MediaType.APPLICATION_JSON)
				.session(session)
				.content(body))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.translatedText").value("xin chào"));
	}

	@org.junit.jupiter.api.Test
	void enhancedTranslate_rateLimitDenied_429() throws Exception {
		when(rateLimitingService.isAllowed(anyString(), eq(RateLimitingService.RateLimitType.AI_TRANSLATION)))
				.thenReturn(false);
		mockMvc.perform(post("/api/ai/chat/enhanced/translate")
				.contentType(MediaType.APPLICATION_JSON)
				.session(session)
				.content("{\"text\":\"hello\",\"sourceLang\":\"auto\",\"targetLang\":\"vi\"}"))
				.andExpect(status().isTooManyRequests());
	}

	@ParameterizedTest(name = "get endpoints: {0}")
	@ValueSource(strings = {"/api/ai/chat/enhanced/rate-limit", 
			"/api/ai/chat/enhanced/languages", 
			"/api/ai/chat/enhanced/health"})
	void simple_get_endpoints_ok(String path) throws Exception {
		when(rateLimitingService.getAllRateLimitInfo(anyString())).thenReturn(java.util.Collections.emptyMap());
		when(translationService.isAvailable()).thenReturn(true);
		when(translationService.getCacheSize()).thenReturn(0);
		mockMvc.perform(get(path).session(session)).andExpect(status().isOk());
	}
}


