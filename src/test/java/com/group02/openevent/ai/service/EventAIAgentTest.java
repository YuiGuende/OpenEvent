package com.group02.openevent.ai.service;

import com.group02.openevent.ai.dto.EventItem;
import com.group02.openevent.ai.mapper.AIEventMapper;
import com.group02.openevent.ai.qdrant.model.ActionType;
import com.group02.openevent.ai.qdrant.service.QdrantService;
import com.group02.openevent.ai.qdrant.service.VectorIntentClassifier;
import com.group02.openevent.ai.security.AISecurityService;
import com.group02.openevent.model.event.Event;
import com.group02.openevent.model.event.Place;
import com.group02.openevent.repository.IUserRepo;
import com.group02.openevent.repository.ai.ChatMessageRepo;
import com.group02.openevent.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class EventAIAgentTest {

    @Mock private EmbeddingService embeddingService;
    @Mock private PlaceService placeService;
    @Mock private EventService eventService;
    @Mock private AgentEventService agentEventService;
    @Mock private VectorIntentClassifier classifier;
    @Mock private WeatherService weatherService;
    @Mock private LLM llm;
    @Mock private QdrantService qdrantService;
    @Mock private EventVectorSearchService eventVectorSearchService;
    @Mock private OrderAIService orderAIService;
    @Mock private IUserRepo userRepo;
    @Mock private TicketTypeService ticketTypeService;
    @Mock private AIEventMapper AIEventMapper;
    @Mock private ChatMessageRepo chatMessageRepo;
    @Mock private LanguageDetectionService languageDetectionService;
    @Mock private TranslationService translationService;
    @Mock private AISecurityService securityService;

    @InjectMocks private EventAIAgent agent;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
    }

    // BR-05: Test invalid time window (start >= end)
    @Test
    void processUserInput_invalidTimeWindow_returnsError() throws Exception {
        // Setup
        String userInput = "Tạo sự kiện Workshop";
        String jsonAction = """
            [
                {
                    "toolName": "ADD_EVENT",
                    "args": {
                        "title": "Workshop",
                        "start_time": "2025-01-15T12:00",
                        "end_time": "2025-01-15T10:00",
                        "place": "Main Hall"
                    }
                }
            ]
            """;
        
        when(embeddingService.getEmbedding(anyString())).thenReturn(new float[384]);
        when(classifier.classifyIntent(anyString(), any())).thenReturn(ActionType.UNKNOWN);
        when(llm.generateResponse(anyList())).thenReturn("Some text " + jsonAction);
        
        Place place = new Place();
        place.setId(1L);
        place.setPlaceName("Main Hall");
        when(placeService.findPlaceByNameFlexible(anyString())).thenReturn(Optional.of(place));
        
        // Execute
        String result = agent.processUserInput(userInput, 1L, "session1", new ArrayList<>(), null);
        
        // Verify - should contain error message about invalid time
        assertTrue(result.contains("Thời gian không hợp lệ") || 
                   result.contains("bắt đầu phải trước kết thúc"),
                   "Result should contain time validation error: " + result);
    }

    // BR-06: Test place not found
    @Test
    void processUserInput_placeNotFound_returnsError() throws Exception {
        // Setup
        String userInput = "Tạo sự kiện Workshop";
        String jsonAction = """
            [
                {
                    "toolName": "ADD_EVENT",
                    "args": {
                        "title": "Workshop",
                        "start_time": "2025-01-15T10:00",
                        "end_time": "2025-01-15T12:00",
                        "place": "NonExistent Place"
                    }
                }
            ]
            """;
        
        when(embeddingService.getEmbedding(anyString())).thenReturn(new float[384]);
        when(classifier.classifyIntent(anyString(), any())).thenReturn(ActionType.UNKNOWN);
        when(llm.generateResponse(anyList())).thenReturn("Some text " + jsonAction);
        when(placeService.findPlaceByNameFlexible(anyString())).thenReturn(Optional.empty());
        when(qdrantService.searchPlacesByVector(any(), anyInt())).thenReturn(Collections.emptyList());
        
        // Execute
        String result = agent.processUserInput(userInput, 1L, "session1", new ArrayList<>(), null);
        
        // Verify - should contain error message about place not found
        assertTrue(result.contains("Không tìm thấy địa điểm") || 
                   result.contains("địa điểm hợp lệ"),
                   "Result should contain place not found error: " + result);
    }

    // BR-07: Test time conflict detection
    @Test
    void processUserInput_timeConflict_returnsConflictMessage() throws Exception {
        // Setup
        String userInput = "Tạo sự kiện Workshop";
        String jsonAction = """
            [
                {
                    "toolName": "ADD_EVENT",
                    "args": {
                        "title": "Workshop",
                        "start_time": "2025-01-15T10:00",
                        "end_time": "2025-01-15T12:00",
                        "place": "Main Hall"
                    }
                }
            ]
            """;
        
        when(embeddingService.getEmbedding(anyString())).thenReturn(new float[384]);
        when(classifier.classifyIntent(anyString(), any())).thenReturn(ActionType.UNKNOWN);
        when(llm.generateResponse(anyList())).thenReturn("Some text " + jsonAction);
        
        Place place = new Place();
        place.setId(1L);
        place.setPlaceName("Main Hall");
        when(placeService.findPlaceByNameFlexible(anyString())).thenReturn(Optional.of(place));
        
        // Mock conflicted event
        Event conflictedEvent = new Event();
        conflictedEvent.setTitle("Existing Event");
        conflictedEvent.setStartsAt(LocalDateTime.parse("2025-01-15T10:00"));
        conflictedEvent.setEndsAt(LocalDateTime.parse("2025-01-15T12:00"));
        when(eventService.isTimeConflict(any(), any(), anyList())).thenReturn(List.of(conflictedEvent));
        
        // Execute
        String result = agent.processUserInput(userInput, 1L, "session1", new ArrayList<>(), null);
        
        // Verify - should contain conflict message
        assertTrue(result.contains("trùng thời gian") || 
                   result.contains("xung đột") ||
                   result.contains("Existing Event"),
                   "Result should contain conflict message: " + result);
    }

    // BR-08: Test weather warning for outdoor event
    @Test
    void processUserInput_outdoorEventWithRainWarning_pausesCreation() throws Exception {
        // Setup
        String userInput = "Tạo festival ngoài trời vào 14:00 ngày 20/01/2025";
        String jsonAction = """
            [
                {
                    "toolName": "ADD_EVENT",
                    "args": {
                        "title": "Festival ngoài trời",
                        "start_time": "2025-01-20T14:00",
                        "end_time": "2025-01-20T18:00",
                        "place": "Công viên",
                        "event_type": "FESTIVAL"
                    }
                }
            ]
            """;
        
        when(embeddingService.getEmbedding(anyString())).thenReturn(new float[384]);
        when(classifier.classifyIntent(anyString(), any())).thenReturn(ActionType.UNKNOWN);
        when(classifier.classifyWeather(anyString(), any())).thenReturn("outdoor_activities");
        when(llm.generateResponse(anyList())).thenReturn("Some text " + jsonAction);
        
        Place place = new Place();
        place.setId(1L);
        place.setPlaceName("Công viên");
        when(placeService.findPlaceByNameFlexible(anyString())).thenReturn(Optional.of(place));
        when(eventService.isTimeConflict(any(), any(), anyList())).thenReturn(Collections.emptyList());
        when(weatherService.getForecastNote(any(), anyString())).thenReturn("Dự báo mưa 60%");
        
        // Execute
        String result = agent.processUserInput(userInput, 1L, "session1", new ArrayList<>(), null);
        
        // Verify - should contain weather warning and ask for confirmation
        assertTrue(result.contains("mưa") || 
                   result.contains("Bạn có muốn tiếp tục"),
                   "Result should contain weather warning: " + result);
    }

    // BR-16: Test user confirms after weather warning
    @Test
    void processUserInput_weatherWarningConfirmed_createsEvent() throws Exception {
        // Setup - simulate pending event exists by creating it first
        String userInput1 = "Tạo festival ngoài trời";
        String jsonAction = """
            [
                {
                    "toolName": "ADD_EVENT",
                    "args": {
                        "title": "Festival ngoài trời",
                        "start_time": "2025-01-20T14:00",
                        "end_time": "2025-01-20T18:00",
                        "place": "Công viên",
                        "event_type": "FESTIVAL"
                    }
                }
            ]
            """;
        
        when(embeddingService.getEmbedding(anyString())).thenReturn(new float[384]);
        when(classifier.classifyIntent(anyString(), any())).thenReturn(ActionType.UNKNOWN);
        when(classifier.classifyWeather(anyString(), any())).thenReturn("outdoor_activities");
        when(llm.generateResponse(anyList())).thenReturn("Some text " + jsonAction);
        
        Place place = new Place();
        place.setId(1L);
        when(placeService.findPlaceByNameFlexible(anyString())).thenReturn(Optional.of(place));
        when(eventService.isTimeConflict(any(), any(), anyList())).thenReturn(Collections.emptyList());
        when(weatherService.getForecastNote(any(), anyString())).thenReturn("Dự báo mưa 60%");
        
        // First call - creates pending event
        agent.processUserInput(userInput1, 1L, "session1", new ArrayList<>(), null);
        
        // Second call - user confirms
        String userInput2 = "có";
        Event savedEvent = new Event();
        savedEvent.setId(1L);
        savedEvent.setTitle("Festival ngoài trời");
        when(AIEventMapper.toEvent(any(EventItem.class))).thenReturn(savedEvent);
        doNothing().when(agentEventService).saveEvent(any(Event.class));
        
        // Execute
        String result = agent.processUserInput(userInput2, 1L, "session1", new ArrayList<>(), null);
        
        // Verify - should create event
        assertTrue(result.contains("Đã tạo sự kiện") || 
                   result.contains("Festival ngoài trời"),
                   "Result should confirm event creation: " + result);
        verify(agentEventService, times(1)).saveEvent(any(Event.class));
    }

    // BR-17: Test user rejects after weather warning
    @Test
    void processUserInput_weatherWarningRejected_cancelsEvent() throws Exception {
        // Setup - simulate pending event exists
        String userInput1 = "Tạo festival ngoài trời";
        String jsonAction = """
            [
                {
                    "toolName": "ADD_EVENT",
                    "args": {
                        "title": "Festival ngoài trời",
                        "start_time": "2025-01-20T14:00",
                        "end_time": "2025-01-20T18:00",
                        "place": "Công viên",
                        "event_type": "FESTIVAL"
                    }
                }
            ]
            """;
        
        when(embeddingService.getEmbedding(anyString())).thenReturn(new float[384]);
        when(classifier.classifyIntent(anyString(), any())).thenReturn(ActionType.UNKNOWN);
        when(classifier.classifyWeather(anyString(), any())).thenReturn("outdoor_activities");
        when(llm.generateResponse(anyList())).thenReturn("Some text " + jsonAction);
        
        Place place = new Place();
        place.setId(1L);
        when(placeService.findPlaceByNameFlexible(anyString())).thenReturn(Optional.of(place));
        when(eventService.isTimeConflict(any(), any(), anyList())).thenReturn(Collections.emptyList());
        when(weatherService.getForecastNote(any(), anyString())).thenReturn("Dự báo mưa 60%");
        
        // First call - creates pending event
        agent.processUserInput(userInput1, 1L, "session1", new ArrayList<>(), null);
        
        // Second call - user rejects
        String userInput2 = "không";
        
        // Execute
        String result = agent.processUserInput(userInput2, 1L, "session1", new ArrayList<>(), null);
        
        // Verify - should cancel event creation
        assertTrue(result.contains("Đã hủy") || 
                   result.contains("từ chối"),
                   "Result should confirm cancellation: " + result);
        verify(agentEventService, never()).saveEvent(any(Event.class));
    }
}

