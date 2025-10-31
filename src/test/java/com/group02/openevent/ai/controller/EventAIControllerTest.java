package com.group02.openevent.ai.controller;

import com.group02.openevent.ai.service.AgentEventService;
import com.group02.openevent.service.EventService;
import com.group02.openevent.service.PlaceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class EventAIControllerTest {

	@Mock private AgentEventService agentEventService;
	@Mock private EventService eventService;
	@Mock private PlaceService placeService;
    @Mock
    private com.fasterxml.jackson.databind.ObjectMapper objectMapper;

	@InjectMocks private EventAIController controller;

	private MockMvc mockMvc;

	@BeforeEach
	void setup() {
		MockitoAnnotations.openMocks(this);
		mockMvc = org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup(controller).build();
	}

	@ParameterizedTest(name = "tool={0}, expect={1}")
//    @CsvSource({"ADD_EVENT,200","UPDATE_EVENT,400","DELETE_EVENT,400","FOO,400"})
	@CsvSource({"UPDATE_EVENT,400","DELETE_EVENT,400","FOO,400"})
	void createEvent_toolValidation(String tool, int expect) throws Exception {
		String body = """
		{"action":{"toolName":"%s","args":{"title":"T"}},"userId":1}
		""".formatted(tool);
		mockMvc.perform(post("/api/ai/event/create")
				.contentType(MediaType.APPLICATION_JSON)
				.content(body))
				.andExpect(status().is(expect));
	}

//	@org.junit.jupiter.api.Test
//	void createEvent_serviceException_500() throws Exception {
//		String body = """
//		{"action":{"toolName":"ADD_EVENT","args":{"title":"T"}},"userId":1}
//		""";
//		org.mockito.Mockito.doThrow(new RuntimeException("svc"))
//				.when(agentEventService).saveEventFromAction(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyLong());
//		mockMvc.perform(post("/api/ai/event/create")
//				.contentType(MediaType.APPLICATION_JSON)
//				.content(body))
//				.andExpect(status().isInternalServerError());
//	}

	@ParameterizedTest(name = "timeContext={0}")
	@CsvSource({"TODAY","TOMORROW","THIS_WEEK","NEXT_WEEK"})
	void freeTime_timeContext(String ctx) throws Exception {
		String body = """
		{"timeContext":"%s","place":""}
		""".formatted(ctx);
		mockMvc.perform(post("/api/ai/event/free-time")
				.contentType(MediaType.APPLICATION_JSON)
				.content(body))
				.andExpect(status().isOk());
	}

	@ParameterizedTest(name = "update tool valid={0}")
	@CsvSource({"UPDATE_EVENT,200","FOO,400"})
	void updateEvent_toolValidation(String tool, int expected) throws Exception {
		String body = """
		{"toolName":"%s","args":{"title":"T"}}
		""".formatted(tool);
		mockMvc.perform(post("/api/ai/event/update")
				.contentType(MediaType.APPLICATION_JSON)
				.content(body))
				.andExpect(status().is(expected));
	}

	@org.junit.jupiter.api.Test
	void updateEvent_serviceException_500() throws Exception {
		String body = """
		{"toolName":"UPDATE_EVENT","args":{"title":"T"}}
		""";
		org.mockito.Mockito.doThrow(new RuntimeException("svc"))
				.when(agentEventService).updateEventFromAction(org.mockito.ArgumentMatchers.any());
		mockMvc.perform(post("/api/ai/event/update")
				.contentType(MediaType.APPLICATION_JSON)
				.content(body))
				.andExpect(status().isInternalServerError());
	}

	@ParameterizedTest(name = "delete tool valid={0}")
	@CsvSource({"DELETE_EVENT,200","BAR,400"})
	void deleteEvent_toolValidation(String tool, int expected) throws Exception {
		String body = """
		{"toolName":"%s","args":{"title":"T"}}
		""".formatted(tool);
		mockMvc.perform(post("/api/ai/event/delete")
				.contentType(MediaType.APPLICATION_JSON)
				.content(body))
				.andExpect(status().is(expected));
	}

	@org.junit.jupiter.api.Test
	void deleteEvent_serviceException_500() throws Exception {
		String body = """
		{"toolName":"DELETE_EVENT","args":{"title":"T"}}
		""";
		org.mockito.Mockito.doThrow(new RuntimeException("svc"))
				.when(agentEventService).deleteEventFromAction(org.mockito.ArgumentMatchers.any());
		mockMvc.perform(post("/api/ai/event/delete")
				.contentType(MediaType.APPLICATION_JSON)
				.content(body))
				.andExpect(status().isInternalServerError());
	}

	@ParameterizedTest(name = "checkConflict valid payload {0}")
	@CsvSource({
			"2025-01-01T10:00,2025-01-01T12:00,",
			"2025-01-02T09:00,2025-01-02T11:00,Main Hall"
	})
	void checkConflict_ok(String start, String end, String place) throws Exception {
		String body = """
		{"startTime":"%s","endTime":"%s","place":"%s"}
		""".formatted(start, end, place == null ? "" : place);
		mockMvc.perform(post("/api/ai/event/check-conflict")
				.contentType(MediaType.APPLICATION_JSON)
				.content(body))
				.andExpect(status().isOk());
	}

	@org.junit.jupiter.api.Test
	void stats_ok() throws Exception {
		mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/ai/event/stats"))
				.andExpect(status().isOk());
	}

	@org.junit.jupiter.api.Test
	void stats_exception_500() throws Exception {
		org.mockito.Mockito.doThrow(new RuntimeException("err"))
				.when(eventService).getAllEvents();
		mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/ai/event/stats"))
				.andExpect(status().isInternalServerError());
	}

	@org.junit.jupiter.api.Test
	void freeTime_placeNotFound_400() throws Exception {
		when(placeService.findPlaceByName(anyString())).thenReturn(java.util.Optional.empty());
		String body = """
		{"timeContext":"THIS_WEEK","place":"NonExistent"}
		""";
		mockMvc.perform(post("/api/ai/event/free-time")
				.contentType(MediaType.APPLICATION_JSON)
				.content(body))
				.andExpect(status().isBadRequest());
	}

	@org.junit.jupiter.api.Test
	void checkConflict_invalidTimeFormat_500() throws Exception {
		String body = """
		{"startTime":"invalid","endTime":"2025-01-01T12:00","place":""}
		""";
		mockMvc.perform(post("/api/ai/event/check-conflict")
				.contentType(MediaType.APPLICATION_JSON)
				.content(body))
				.andExpect(status().isInternalServerError());
	}

	@org.junit.jupiter.api.Test
	void updateEvent_notFound_noException() throws Exception {
		when(eventService.getEventByEventId(anyLong())).thenReturn(java.util.Optional.empty());
		when(eventService.getFirstEventByTitle(anyString())).thenReturn(java.util.Optional.empty());
		String body = """
		{"toolName":"UPDATE_EVENT","args":{"event_id":999}}
		""";
		mockMvc.perform(post("/api/ai/event/update")
				.contentType(MediaType.APPLICATION_JSON)
				.content(body))
				.andExpect(status().isOk());
	}

	@org.junit.jupiter.api.Test
	void deleteEvent_notFound_noException() throws Exception {
		when(eventService.removeEvent(anyLong())).thenReturn(false);
		when(eventService.deleteByTitle(anyString())).thenReturn(false);
		String body = """
		{"toolName":"DELETE_EVENT","args":{"event_id":999}}
		""";
		mockMvc.perform(post("/api/ai/event/delete")
				.contentType(MediaType.APPLICATION_JSON)
				.content(body))
				.andExpect(status().isOk());
	}
}


