package com.group02.openevent.ai.controller;

import com.group02.openevent.ai.service.LibreTranslateService;
import com.group02.openevent.ai.service.TranslationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TranslationControllerTest {

	@Mock private TranslationService translationService;
	@Mock private LibreTranslateService libreTranslateService;

	@InjectMocks private TranslationController controller;

	private MockMvc mockMvc;

	@BeforeEach
	void setup() {
		MockitoAnnotations.openMocks(this);
		mockMvc = org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup(controller).build();
	}

	@ParameterizedTest(name = "source={0}, target={1}, valid={2}")
	@CsvSource({"en,vi,true","xx,vi,false","en,yy,false"})
	void translate_validateLang(String source, String target, boolean valid) throws Exception {
		when(translationService.translate(anyString(), any(), any())).thenReturn("ok");
		var req = post("/api/ai/translation/translate")
				.param("text","hello").param("sourceLang", source).param("targetLang", target);
		if (valid) {
			mockMvc.perform(req).andExpect(status().isOk()).andExpect(jsonPath("$.translatedText").value("ok"));
		} else {
			mockMvc.perform(req).andExpect(status().isBadRequest());
		}
	}

	@ParameterizedTest
	@CsvSource({"en,vi"})
	void translate_async_ok(String source, String target) throws Exception {
		when(translationService.translateAsync(anyString(), any(), any()))
				.thenReturn(java.util.concurrent.CompletableFuture.completedFuture("ok"));
		mockMvc.perform(post("/api/ai/translation/translate-async")
				.param("text","hello").param("sourceLang", source).param("targetLang", target))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.translatedText").value("ok"));
	}

	@org.junit.jupiter.api.Test
	void translate_async_exceptionally_500() throws Exception {
		when(translationService.translateAsync(anyString(), any(), any()))
				.thenReturn(java.util.concurrent.CompletableFuture.failedFuture(new RuntimeException("err")));
		mockMvc.perform(post("/api/ai/translation/translate-async")
				.param("text","hello").param("sourceLang","en").param("targetLang","vi"))
				.andExpect(status().isInternalServerError());
	}

	@org.junit.jupiter.api.Test
	void languages_test_status_cache() throws Exception {
		when(translationService.testConnection()).thenReturn(true);
		when(translationService.isAvailable()).thenReturn(true);
		when(translationService.getCacheSize()).thenReturn(0);
		mockMvc.perform(get("/api/ai/translation/test")).andExpect(status().isOk());
		mockMvc.perform(get("/api/ai/translation/status")).andExpect(status().isOk());
	}

	@org.junit.jupiter.api.Test
	void clear_cache_ok() throws Exception {
		mockMvc.perform(post("/api/ai/translation/clear-cache")).andExpect(status().isOk());
	}

	@org.junit.jupiter.api.Test
	void translate_serviceException_500() throws Exception {
		when(translationService.translate(anyString(), any(), any())).thenThrow(new RuntimeException("err"));
		mockMvc.perform(post("/api/ai/translation/translate")
				.param("text","hello").param("sourceLang","en").param("targetLang","vi"))
				.andExpect(status().isInternalServerError())
				.andExpect(jsonPath("$.error").exists());
	}

	@org.junit.jupiter.api.Test
	void languages_exceptionally_500() throws Exception {
		when(libreTranslateService.getSupportedLanguages())
				.thenReturn(java.util.concurrent.CompletableFuture.failedFuture(new RuntimeException("err")));
		mockMvc.perform(get("/api/ai/translation/languages"))
				.andExpect(status().isInternalServerError())
				.andExpect(jsonPath("$.error").exists());
	}

	@org.junit.jupiter.api.Test
	void testConnection_exception_500() throws Exception {
		when(translationService.testConnection()).thenThrow(new RuntimeException("err"));
		mockMvc.perform(get("/api/ai/translation/test"))
				.andExpect(status().isInternalServerError());
	}

	@org.junit.jupiter.api.Test
	void clearCache_exception_500() throws Exception {
		org.mockito.Mockito.doThrow(new RuntimeException("err")).when(translationService).clearCache();
		mockMvc.perform(post("/api/ai/translation/clear-cache"))
				.andExpect(status().isInternalServerError());
	}

	@org.junit.jupiter.api.Test
	void status_exception_500() throws Exception {
		when(translationService.isAvailable()).thenThrow(new RuntimeException("err"));
		mockMvc.perform(get("/api/ai/translation/status"))
				.andExpect(status().isInternalServerError());
	}

	@org.junit.jupiter.api.Test
	void aiResponse_serviceException_500() throws Exception {
		when(translationService.translateAIResponse(anyString(), any())).thenThrow(new RuntimeException("err"));
		mockMvc.perform(post("/api/ai/translation/ai-response")
				.param("aiResponse","h").param("userLanguage","en"))
				.andExpect(status().isInternalServerError());
	}

	@org.junit.jupiter.api.Test
	void userInput_serviceException_500() throws Exception {
		when(translationService.translateUserInput(anyString(), any())).thenThrow(new RuntimeException("err"));
		mockMvc.perform(post("/api/ai/translation/user-input")
				.param("userInput","h").param("userLanguage","en"))
				.andExpect(status().isInternalServerError());
	}

	@ParameterizedTest
	@CsvSource({"en,true","xx,false"})
	void aiResponse_param(String userLang, boolean valid) throws Exception {
		when(translationService.translateAIResponse(anyString(), any())).thenReturn("done");
		var req = post("/api/ai/translation/ai-response").param("aiResponse","hello").param("userLanguage", userLang);
		if (valid) mockMvc.perform(req).andExpect(status().isOk()).andExpect(jsonPath("$.translatedResponse").value("done"));
		else mockMvc.perform(req).andExpect(status().isBadRequest());
	}

	@ParameterizedTest
	@CsvSource({"en,true","xx,false"})
	void userInput_param(String userLang, boolean valid) throws Exception {
		when(translationService.translateUserInput(anyString(), any())).thenReturn("done");
		var req = post("/api/ai/translation/user-input").param("userInput","hello").param("userLanguage", userLang);
		if (valid) mockMvc.perform(req).andExpect(status().isOk()).andExpect(jsonPath("$.translatedInput").value("done"));
		else mockMvc.perform(req).andExpect(status().isBadRequest());
	}
}


