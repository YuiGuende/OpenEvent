package com.group02.openevent.ai.controller;

import com.group02.openevent.repository.IChatHistoryRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ChatControllerTest {

	@Mock private IChatHistoryRepo chatHistoryRepo;
	@InjectMocks private ChatController controller;

	private MockMvc mockMvc;

	@BeforeEach
	void setup() {
		MockitoAnnotations.openMocks(this);
		mockMvc = org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup(controller).build();
	}

	@ParameterizedTest(name = "withSession={0}")
	@ValueSource(booleans = {true, false})
	void history_parametrized(boolean withSession) throws Exception {
		if (withSession) {
			mockMvc.perform(get("/api/ai/chat/history/{userId}", 5).param("sessionId","S1"))
					.andExpect(status().isOk());
		} else {
			mockMvc.perform(get("/api/ai/chat/history/{userId}", 5))
					.andExpect(status().isOk());
		}
	}

	@org.junit.jupiter.api.Test
	void history_repoException_500() throws Exception {
		org.mockito.Mockito.doThrow(new RuntimeException("db"))
				.when(chatHistoryRepo).findByUserIdOrderByTimestampAsc(org.mockito.ArgumentMatchers.anyInt());
		mockMvc.perform(get("/api/ai/chat/history/{userId}", 9))
				.andExpect(status().isInternalServerError());
	}

	@ParameterizedTest
	@ValueSource(ints = {1, 5, 10})
	void sessions_parametrized(int userId) throws Exception {
		mockMvc.perform(get("/api/ai/chat/sessions/{userId}", userId))
				.andExpect(status().isOk());
	}

	@org.junit.jupiter.api.Test
	void sessions_repoException_500() throws Exception {
		org.mockito.Mockito.doThrow(new RuntimeException("db"))
				.when(chatHistoryRepo).findDistinctSessionIdsByUserId(org.mockito.ArgumentMatchers.anyInt());
		mockMvc.perform(get("/api/ai/chat/sessions/{userId}", 3))
				.andExpect(status().isInternalServerError());
	}

	@ParameterizedTest
	@ValueSource(strings = {"S1", "S2"})
	void clear_history(String sid) throws Exception {
		mockMvc.perform(delete("/api/ai/chat/history/{user}/{sid}", 5, sid))
				.andExpect(status().isOk());
	}

	@org.junit.jupiter.api.Test
	void clear_history_repoException_500() throws Exception {
		org.mockito.Mockito.doThrow(new RuntimeException("db"))
				.when(chatHistoryRepo).deleteByUserIdAndSessionId(org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.anyString());
		mockMvc.perform(delete("/api/ai/chat/history/{user}/{sid}", 5, "S1"))
				.andExpect(status().isInternalServerError());
	}

	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	void session_info_and_new(boolean createNew) throws Exception {
		mockMvc.perform(get("/api/ai/chat/session-info"))
				.andExpect(status().isOk());
		if (createNew) {
			mockMvc.perform(post("/api/ai/chat/new-session"))
					.andExpect(status().isOk());
		}
	}

	@ParameterizedTest
	@ValueSource(ints = {1, 2})
	void stats_ok(int userId) throws Exception {
		mockMvc.perform(get("/api/ai/chat/stats/{userId}", userId))
				.andExpect(status().isOk());
	}

	@org.junit.jupiter.api.Test
	void stats_exception_500() throws Exception {
		org.mockito.Mockito.doThrow(new RuntimeException("db"))
				.when(chatHistoryRepo).findByUserIdOrderByTimestampAsc(org.mockito.ArgumentMatchers.anyInt());
		mockMvc.perform(get("/api/ai/chat/stats/{userId}", 9))
				.andExpect(status().isInternalServerError());
	}

	@org.junit.jupiter.api.Test
	void getSessionInfo_withSessionId() throws Exception {
		var session = new org.springframework.mock.web.MockHttpSession();
		session.setAttribute("sessionId", "S123");
		mockMvc.perform(get("/api/ai/chat/session-info").session(session))
				.andExpect(status().isOk())
				.andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.sessionId").value("S123"));
	}
}


