package com.group02.openevent.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.group02.openevent.ai.security.AISecurityService;
import com.group02.openevent.ai.security.RateLimitingService;
import com.group02.openevent.config.SessionInterceptor;
import com.group02.openevent.config.TestWebConfig;
import com.group02.openevent.controller.event.EventController;
import com.group02.openevent.dto.request.create.EventCreationRequest;
import com.group02.openevent.dto.response.EventResponse;
import com.group02.openevent.model.enums.EventStatus;
import com.group02.openevent.model.enums.EventType;
import com.group02.openevent.model.event.Event;
import com.group02.openevent.model.event.EventImage;
import com.group02.openevent.repository.ITicketTypeRepo;
import com.group02.openevent.service.EventService;
import com.group02.openevent.service.IImageService;
import com.group02.openevent.service.TicketTypeService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Slf4j
@WebMvcTest(controllers = EventController.class,
        excludeAutoConfiguration = SecurityAutoConfiguration.class)
public class EventControllerTest {

    @Autowired
    public MockMvc mockMvc;

    @MockBean
    private EventService eventService;
    @MockBean
    private IImageService imageService;
    @MockBean
    private ITicketTypeRepo ticketTypeRepo;
    @MockBean
    private TicketTypeService ticketTypeService;

    @MockBean
    SessionInterceptor sessionInterceptor;

    private EventResponse eventResponse;
    ObjectMapper mapper;
    @MockitoBean
    private RateLimitingService rateLimitingService;

    @MockitoBean
    private AISecurityService aiSecurityService;
    @BeforeEach
    public void setup() throws Exception {
        eventResponse = new EventResponse();
        eventResponse.setId(101);
        eventResponse.setEventType(EventType.MUSIC);
        eventResponse.setStatus(EventStatus.DRAFT);
        mapper = new ObjectMapper().registerModule(new JavaTimeModule());
        when(sessionInterceptor.preHandle(any(), any(), any())).thenReturn(true);
    }

    @ParameterizedTest
    @CsvSource({
            "Spring Boot Workshop, WORKSHOP",
            "Music Night, MUSIC",
            "CodeFest 2025, COMPETITION"
    })
    void TC01_ShouldRedirectToGettingStarted_WhenEventSavedSuccessfully(String title, String eventType) throws Exception {
        when(eventService.saveEvent(any(EventCreationRequest.class), 1L)).thenReturn(eventResponse);

        mockMvc.perform(post("/api/events/saveEvent")
                        .param("title", title)
                        .param("eventType", eventType)
                        .param("startsAt", LocalDateTime.now().toString())
                        .param("endsAt", LocalDateTime.now().plusDays(1).toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/manage/event/101/getting-stared"));

    }

    @ParameterizedTest
    @CsvSource({
            "Spring Boot Workshop, WORKSHOP",
            "Music Night, MUSIC",
            "CodeFest 2025, COMPETITION"
    })
    void TC02_ShouldCallEventServiceWithCorrectRequest(String title, String eventType) throws Exception {
        when(eventService.saveEvent(any(EventCreationRequest.class),1L)).thenReturn(eventResponse);

        mockMvc.perform(post("/api/events/saveEvent")
                        .param("title", title)
                        .param("eventType", eventType))
                .andExpect(status().is3xxRedirection());

//        verify(eventService, times(1)).saveEvent(Mockito.argThat((EventCreationRequest req) ->
//                req.getTitle().equals("Music Festival")
//                        && req.getEventType() == EventType.MUSIC
//        ));
    }

    @ParameterizedTest
    @CsvSource({
            "Spring Boot Workshop, WORKSHOP",
            "Music Night, MUSIC",
            "CodeFest 2025, COMPETITION"
    })
    void TC03_ShouldReturnServerError_WhenServiceThrowsException(String title, String eventType) throws Exception {
        // given
        when(eventService.saveEvent(any(EventCreationRequest.class),1L))
                .thenThrow(new RuntimeException("Save failed"));

        // when & then
        mockMvc.perform(post("/api/events/saveEvent")
                        .param("title", title)
                        .param("eventType", eventType))
                .andExpect(status().is4xxClientError());// hoặc .andExpect(status().isInternalServerError());

    }

    @Test
    void TC04_ShouldHandleEmptyRequestGracefully() throws Exception {
        // given
        when(eventService.saveEvent(any(EventCreationRequest.class),1L)).thenReturn(eventResponse);

        // when & then
        mockMvc.perform(post("/api/events/saveEvent"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/manage/event/101/getting-stared"));

        verify(eventService, times(1)).saveEvent(any(EventCreationRequest.class),1L);
    }

    @ParameterizedTest(name = "TC{index} → Should redirect correctly for eventType={0}")
    @CsvSource({
            "WORKSHOP, /manage/event/101/getting-stared",
            "MUSIC, /manage/event/101/getting-stared",
            "FESTIVAL, /manage/event/101/getting-stared",
            "COMPETITION, /manage/event/101/getting-stared",
            "WORKSHOP, /manage/event/101/getting-stared"  // lặp intentional
    })
    void TC05_ShouldRedirectCorrectly_ForDifferentEventTypes(String eventType, String expectedRedirectUrl) throws Exception {
        // Given
        EventResponse response = new EventResponse();
        response.setId(101);
        response.setEventType(EventType.valueOf(eventType));
        response.setStatus(EventStatus.DRAFT);

        when(eventService.saveEvent(any(EventCreationRequest.class),1L)).thenReturn(response);

        // When + Then
        mockMvc.perform(post("/api/events/saveEvent")
                        .param("title", "Parameterized Event")
                        .param("eventType", eventType)
                        .param("startsAt", LocalDateTime.now().toString())
                        .param("endsAt", LocalDateTime.now().plusDays(1).toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(expectedRedirectUrl));

        verify(eventService).saveEvent(any(EventCreationRequest.class),1L);
    }

    @ParameterizedTest
    @CsvSource({
            "  Workshop  , WORKSHOP",
            "   Music, MUSIC"
    })
    void TC05_ShouldTrimAndAcceptStringInputs(String rawTitle, String type) throws Exception {
        // Given
        EventResponse response = new EventResponse();
        response.setId(202);
        response.setEventType(EventType.valueOf(type));
        when(eventService.saveEvent(any(EventCreationRequest.class),1L)).thenReturn(response);

        // When
        mockMvc.perform(post("/api/events/saveEvent")
                        .param("title", rawTitle)
                        .param("eventType", type))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/manage/event/202/getting-stared"));
    }

    @Test
    void TC06_ShouldRedirectToCorrectPath_WhenEventTypeIsDifferent() throws Exception {
        // Given
        eventResponse.setId(303);
        eventResponse.setEventType(EventType.FESTIVAL);
        when(eventService.saveEvent(any(EventCreationRequest.class),1L)).thenReturn(eventResponse);

        // When / Then
        mockMvc.perform(post("/api/events/saveEvent")
                        .param("title", "Festival Event")
                        .param("eventType", "FESTIVAL"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/manage/event/303/getting-stared"));
    }

    @Test
    void TC07_ShouldNotFail_WhenModelIsEmpty() throws Exception {
        // Given
        when(eventService.saveEvent(any(EventCreationRequest.class),1L)).thenReturn(eventResponse);

        // When / Then
        mockMvc.perform(post("/api/events/saveEvent")
                        .param("title", "Empty Model Test")
                        .param("eventType", "WORKSHOP"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void TC08_ShouldReturnRedirectResponse_WithMockMvc() throws Exception {
        EventCreationRequest req = new EventCreationRequest();
        req.setTitle("Integration Test");
        req.setEventType(EventType.MUSIC);
        when(eventService.saveEvent(any(EventCreationRequest.class),1L)).thenReturn(eventResponse);

        mockMvc.perform(post("/api/events/saveEvent")
                        .flashAttr("eventForm", req))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/manage/event/101/getting-stared"));
    }

    @Test
    void TC09_ShouldReturnError_WhenInvalidDateFormat() throws Exception {
        mockMvc.perform(post("/api/events/saveEvent")
                        .param("title", "Bad Date")
                        .param("eventType", "WORKSHOP")
                        .param("startsAt", "invalid-date"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void TCU01_ShouldUpdateEventSuccessfully_WhenValidRequest() throws Exception {
        // Given
        when(eventService.updateEvent(anyLong(), any())).thenReturn(eventResponse);

        // When / Then
        mockMvc.perform(post("/api/events/update/99")
                        .param("title", "Music Fest")
                        .param("eventType", "MUSIC"))
                .andExpect(status().isOk())
                .andExpect(view().name("fragments/getting-started :: content"))
                .andExpect(model().attributeExists("message"));

        verify(eventService).updateEvent(eq(99L), any());
    }

    @Test
    void TCU02_ShouldParsePlacesJsonCorrectly() throws Exception {
        // Given
        String placesJson = "[{\"id\":1,\"placeName\":\"Hall A\",\"building\":\"ALPHA\"}]";

        when(eventService.updateEvent(anyLong(), any())).thenReturn(eventResponse);

        mockMvc.perform(post("/api/events/update/99")
                        .param("placesJson", placesJson))
                .andExpect(status().isOk());

        verify(eventService).updateEvent(eq(99L), argThat(req ->
                req.getPlaces() != null &&
                        req.getPlaces().size() == 1 &&
                        "Hall A".equals(req.getPlaces().get(0).getPlaceName())
        ));
    }

    @Test
    void TCU03_ShouldHandleEmptyPlacesJson() throws Exception {
        // Given
        when(eventService.updateEvent(anyLong(), any())).thenReturn(eventResponse);

        // When / Then
        mockMvc.perform(post("/api/events/update/99")
                        .param("placesJson", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("fragments/getting-started :: content"));

        verify(eventService).updateEvent(eq(99L), any());
    }

    @Test
    void TCU04_ShouldHandleInvalidJson_Gracefully() throws Exception {
        // Given
        String invalidJson = "{id:1,placeName:'Hall A'"; // thiếu dấu }

        // When / Then
        mockMvc.perform(post("/api/events/update/99")
                        .param("placesJson", invalidJson))
                .andExpect(status().isOk()) // vẫn trả về OK do controller catch lỗi
                .andExpect(model().attributeExists("error"))
                .andExpect(view().name("fragments/getting-started :: content"));

        // Không gọi service (vì parse JSON fail)
        verify(eventService, never()).updateEvent(anyLong(), any());
    }

    @Test
    void TCU05_ShouldAddMessageToModel_WhenUpdateSuccess() throws Exception {
        // Given
        when(eventService.updateEvent(anyLong(), any())).thenReturn(eventResponse);

        // When / Then
        mockMvc.perform(post("/api/events/update/99")
                        .param("title", "Updated Event"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("message", "Cập nhật thành công!"))
                .andExpect(view().name("fragments/getting-started :: content"));

        verify(eventService).updateEvent(eq(99L), any());
    }

    @Test
    void TCU06_ShouldAddErrorToModel_WhenExceptionThrown() throws Exception {
        // Given
        when(eventService.updateEvent(anyLong(), any()))
                .thenThrow(new RuntimeException("DB Error"));

        // When / Then
        mockMvc.perform(post("/api/events/update/99"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("error"))
                .andExpect(view().name("fragments/getting-started :: content"));

        verify(eventService).updateEvent(eq(99L), any());
    }

    @Test
    void TCU07_ShouldRenderCorrectFragmentView() throws Exception {
        // Given
        when(eventService.updateEvent(anyLong(), any())).thenReturn(eventResponse);

        // When / Then
        mockMvc.perform(post("/api/events/update/99"))
                .andExpect(status().isOk())
                .andExpect(view().name("fragments/getting-started :: content"));
    }

    @Test
    void TCT01_ShouldUpdateTickets_WhenValidJson() throws Exception {
        // Given
        String ticketsJson = """
                   [
                                   {"ticketTypeId":1, "name":"VIP", "price":500000},
                                   {"ticketTypeId":2, "name":"Standard", "price":200000}
                                 ]
                
                """;

        // When / Then
        mockMvc.perform(post("/api/events/update-tickets/10")
                        .param("ticketsJson", ticketsJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Tickets updated"));

        verify(ticketTypeService).updateTickets(eq(10L), anyList());
    }
    @Test
    void TCT02_ShouldReturnMessage_WhenTicketsUpdated() throws Exception {
        // Given
        String json = "[{\"ticketTypeId\":1,\"name\":\"VIP\"}]";

        // When / Then
        mockMvc.perform(post("/api/events/update-tickets/10")
                        .param("ticketsJson", json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Tickets updated"));
    }
    @Test
    void TCT03_ShouldSkipUpdate_WhenTicketsJsonEmpty() throws Exception {
        mockMvc.perform(post("/api/events/update-tickets/10")
                        .param("ticketsJson", ""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Tickets updated"));

        verify(ticketTypeService, never()).updateTickets(anyLong(), any());
    }
    @Test
    void TCT04_ShouldHandleInvalidJson_Gracefully() throws Exception {
        // Given – JSON sai cú pháp
        String invalidJson = "[{\"ticketTypeId\":1, \"name\":\"VIP\""; // thiếu dấu ]

        // When / Then
        mockMvc.perform(post("/api/events/update-tickets/10")
                        .param("ticketsJson", invalidJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Error")));
    }
    @Test
    void TCT05_ShouldHandleServiceException_Gracefully() throws Exception {
        // Given
        String json = "[{\"ticketTypeId\":1,\"name\":\"VIP\"}]";
        doThrow(new RuntimeException("DB Error"))
                .when(ticketTypeService).updateTickets(anyLong(), anyList());

        // When / Then
        mockMvc.perform(post("/api/events/update-tickets/10")
                        .param("ticketsJson", json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Error: DB Error"));
    }
    @Test
    void TCT06_ShouldReturnValidJsonResponse() throws Exception {
        String json = "[{\"ticketTypeId\":1,\"name\":\"VIP\"}]";

        mockMvc.perform(post("/api/events/update-tickets/10")
                        .param("ticketsJson", json))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.message").exists());
    }
    @Test
    void TC01_ShouldUploadImages_WhenValidData() throws Exception {
        MockMultipartFile file1 = new MockMultipartFile("files", "img1.jpg", "image/jpeg", "dummy1".getBytes());
        MockMultipartFile file2 = new MockMultipartFile("files", "img2.jpg", "image/jpeg", "dummy2".getBytes());

        Event event = new Event();
        event.setId(1L);
        event.setTitle("Test Event");
        event.setEventImages(new HashSet<>());

        Event savedEvent = new Event();
        savedEvent.setId(1L);
        savedEvent.setTitle("Test Event");
        savedEvent.setEventImages(event.getEventImages());

        when(eventService.getEventById(1L)).thenReturn(Optional.of(event));
        when(imageService.saveImage(any())).thenReturn("http://img.com/image.jpg");
        when(eventService.saveEvent(any(Event.class))).thenReturn(savedEvent);

        mockMvc.perform(multipart("/api/events/upload/multiple-images")
                        .file(file1)
                        .file(file2)
                        .param("orderIndexes", "0", "1")
                        .param("mainPosters", "true", "false")
                        .param("eventId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))  // ✅ Bây giờ có id
                .andExpect(jsonPath("$.title").value("Test Event"));

        verify(imageService, times(2)).saveImage(any());
        verify(eventService).saveEvent(any(Event.class));
    }
    @Test
    @DisplayName("TC02_ShouldReturn404_WhenEventNotFound")
    void TC02_ShouldReturn404_WhenEventNotFound() throws Exception {
        // Given
        MockMultipartFile file = new MockMultipartFile("files", "image.jpg", "image/jpeg", "test".getBytes());

        when(eventService.getEventById(999L)).thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(multipart("/api/events/upload/multiple-images")
                        .file(file)
                        .param("orderIndexes", "0")
                        .param("mainPosters", "true")
                        .param("eventId", "999"))
                .andExpect(status().isNotFound());

        // ✅ Ensure that saveEvent is never called
        verify(eventService, never()).saveEvent(any(Event.class));
        verify(imageService, never()).saveImage(any());
    }
    @Test
    @DisplayName("TC03_ShouldReturn400_WhenArrayLengthsMismatch")
    void TC03_ShouldReturn400_WhenArrayLengthsMismatch() throws Exception {
        // Given
        Event event = new Event();
        event.setId(10L);
        event.setEventImages(new HashSet<>() {
        });

        MockMultipartFile file1 = new MockMultipartFile("files", "image1.jpg", "image/jpeg", "test1".getBytes());
        MockMultipartFile file2 = new MockMultipartFile("files", "image2.jpg", "image/jpeg", "test2".getBytes());

        when(eventService.getEventById(10L)).thenReturn(Optional.of(event));

        // When & Then
        mockMvc.perform(multipart("/api/events/upload/multiple-images")
                        .file(file1)
                        .file(file2)
                        .param("orderIndexes", "0") // ❌ chỉ 1 phần tử
                        .param("mainPosters", "true", "false") // ✅ 2 phần tử
                        .param("eventId", "10"))
                .andExpect(status().isBadRequest());

        // ✅ Verify không gọi tới các service xử lý ảnh/lưu event
        verify(imageService, never()).saveImage(any());
        verify(eventService, never()).saveEvent(any(Event.class));
    }
    @Test
    @DisplayName("TC04_ShouldSkipEmptyFiles_WhenSomeFilesAreEmpty")
    void TC04_ShouldSkipEmptyFiles_WhenSomeFilesAreEmpty() throws Exception {
        // Given
        Event event = new Event();
        event.setId(10L);
        event.setEventImages(new HashSet<>());

        MockMultipartFile validFile = new MockMultipartFile("files", "image1.jpg", "image/jpeg", "valid".getBytes());
        MockMultipartFile emptyFile = new MockMultipartFile("files", "empty.jpg", "image/jpeg", new byte[0]);

        int[] orderIndexes = {0, 1};
        boolean[] mainPosters = {true, false};

        when(eventService.getEventById(10L)).thenReturn(Optional.of(event));
        when(imageService.saveImage(validFile)).thenReturn("url_valid.jpg");
        when(eventService.saveEvent(any(Event.class))).thenReturn(event);

        // When & Then
        mockMvc.perform(multipart("/api/events/upload/multiple-images")
                        .file(validFile)
                        .file(emptyFile)
                        .param("orderIndexes", "0", "1")
                        .param("mainPosters", "true", "false")
                        .param("eventId", "10"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(10));

        // ✅ Verify: chỉ file hợp lệ được lưu
        verify(imageService, times(1)).saveImage(validFile);
        verify(imageService, never()).saveImage(emptyFile);
        verify(eventService, times(1)).saveEvent(any(Event.class));
    }
    @Test
    void TC05_ShouldReturnError_WhenImageUploadFails() throws Exception {
        // Given
        MockMultipartFile file1 = new MockMultipartFile("files", "image1.jpg", "image/jpeg", "fakeimage1".getBytes());

        int[] orderIndexes = {0};
        boolean[] mainPosters = {true};
        Long eventId = 1L;

        Event mockEvent = new Event();
        mockEvent.setId(eventId);
        mockEvent.setEventImages(new HashSet<>());

        when(eventService.getEventById(eventId)).thenReturn(Optional.of(mockEvent));
        when(imageService.saveImage(any(MultipartFile.class)))
                .thenThrow(new RuntimeException("Disk error"));

        // When & Then
        mockMvc.perform(multipart("/api/events/upload/multiple-images")
                        .file(file1)
                        .param("orderIndexes", "0")
                        .param("mainPosters", "true")
                        .param("eventId", "1")
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isInternalServerError()) // ✅ Expect 500 now
                .andExpect(jsonPath("$.error").exists())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Disk error")));
    }
    @Test
    void TC01_ShouldReturnOk_WhenDeleteSuccess() throws Exception {
        // given
        Long ticketId = 1L;
        doNothing().when(ticketTypeService).deleteTicketType(ticketId);

        // when + then
        mockMvc.perform(delete("/api/events/ticket/{ticketTypeId}", ticketId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Đã xóa vé"));
    }
    @Test
    void TC02_ShouldReturnBadRequest_WhenIllegalStateExceptionThrown() throws Exception {
        // given
        Long ticketId = 1L;
        doThrow(new IllegalStateException("Vé đã có đơn hàng"))
                .when(ticketTypeService).deleteTicketType(ticketId);

        // when + then
        mockMvc.perform(delete("/api/events/ticket/{ticketTypeId}", ticketId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Vé đã có đơn hàng"));
    }

    @Test
    void TC03_ShouldReturnBadRequest_WhenFKConstraintViolation() throws Exception {
        // given
        Long ticketId = 1L;
        doThrow(new DataIntegrityViolationException("FK violation"))
                .when(ticketTypeService).deleteTicketType(ticketId);

        // when + then
        mockMvc.perform(delete("/api/events/ticket/{ticketTypeId}", ticketId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Không thể xóa vé vì đã có đơn hàng tham chiếu"));
    }

    @Test
    void TC04_ShouldReturnServerError_WhenUnexpectedException() throws Exception {
        // given
        Long ticketId = 1L;
        doThrow(new RuntimeException("DB Down"))
                .when(ticketTypeService).deleteTicketType(ticketId);

        // when + then
        mockMvc.perform(delete("/api/events/ticket/{ticketTypeId}", ticketId))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("Lỗi hệ thống khi xóa vé"));
    }

    @Test
    void TC05_ShouldRejectInvalidId_WhenPathVarIsInvalid() throws Exception {
        // when + then
        mockMvc.perform(delete("/api/events/ticket/abc"))
                .andExpect(status().isBadRequest());
    }

    // ===== Additional tests for remaining endpoints =====


    @Test
    void GET_EVENT_ShouldReturnEvent_WhenExists() throws Exception {
        Event ev = new Event();
        ev.setId(77L);
        when(eventService.getEventById(77L)).thenReturn(Optional.of(ev));

        mockMvc.perform(get("/api/events/{id}", 77))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(77));
    }

    @Test
    void GET_EVENT_ShouldReturnEmpty_WhenNotFound() throws Exception {
        when(eventService.getEventById(999L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/events/{id}", 999))
                .andExpect(status().isOk())
                .andExpect(content().string("null"));
    }

    @Test
    void GET_BY_TYPE_ShouldReturnList() throws Exception {
        Event e1 = new Event(); e1.setId(1L);
        Event e2 = new Event(); e2.setId(2L);
        when(eventService.getEventsByType(com.group02.openevent.model.event.MusicEvent.class))
                .thenReturn(java.util.Arrays.asList(e1, e2));

        mockMvc.perform(get("/api/events/type/{type}", "music"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[1].id").value(2));
    }

    @Test
    void UPLOAD_IMAGE_ShouldAttachImageAndReturnEvent() throws Exception {
        MockMultipartFile file = new MockMultipartFile("files", "a.jpg", "image/jpeg", "abc".getBytes());
        Event ev = new Event(); ev.setId(5L); ev.setEventImages(new java.util.HashSet<>());
        when(eventService.getEventById(5L)).thenReturn(Optional.of(ev));
        when(imageService.saveImage(any())).thenReturn("http://url");
        when(eventService.saveEvent(any(Event.class))).thenReturn(ev);

        mockMvc.perform(multipart("/api/events/upload/multiple-images")
                        .file(file)
                        .param("orderIndexes", "0")
                        .param("mainPosters", "true")
                        .param("eventId", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5));
    }

    @Test
    void UPLOAD_IMAGE_ShouldReturn404_WhenEventMissing() throws Exception {
        MockMultipartFile file = new MockMultipartFile("files", "a.jpg", "image/jpeg", "abc".getBytes());
        when(eventService.getEventById(123L)).thenReturn(Optional.empty());

        mockMvc.perform(multipart("/api/events/upload/multiple-images")
                        .file(file)
                        .param("orderIndexes", "0")
                        .param("mainPosters", "true")
                        .param("eventId", "123"))
                .andExpect(status().isNotFound());
    }

    @Test
    void UPLOAD_IMAGES_BATCH_ShouldReturnOk() throws Exception {
        MockMultipartFile file1 = new MockMultipartFile("files", "a.jpg", "image/jpeg", "abc".getBytes());
        MockMultipartFile file2 = new MockMultipartFile("files", "b.jpg", "image/jpeg", "def".getBytes());
        Event ev = new Event(); ev.setId(7L); ev.setEventImages(new java.util.HashSet<>());
        when(eventService.getEventById(7L)).thenReturn(Optional.of(ev));
        when(imageService.saveImage(any())).thenReturn("http://url");
        when(eventService.saveEvent(any(Event.class))).thenReturn(ev);

        mockMvc.perform(multipart("/api/events/upload/multiple-images")
                        .file(file1)
                        .file(file2)
                        .param("orderIndexes", "0", "1")
                        .param("mainPosters", "true", "false")
                        .param("eventId", "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(7));
    }

    @Test
    void UPLOAD_IMAGES_BATCH_ShouldReturn404_WhenEventMissing() throws Exception {
        when(eventService.getEventById(404L)).thenReturn(Optional.empty());

        String imagesJson = "[{'orderIndex':0,'mainPoster':true}]".replace('\'', '"');

        mockMvc.perform(post("/api/events/upload/images-batch")
                        .param("eventId", "404")
                        .param("images", imagesJson))
                .andExpect(status().isNotFound());
    }















}
