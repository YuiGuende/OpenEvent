package com.group02.openevent.controller.event;

import com.group02.openevent.dto.event.WorkshopEventDetailDTO;
import com.group02.openevent.service.IWorkshopService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@ExtendWith(MockitoExtension.class)
@DisplayName("WorkshopController Integration Tests (Full Coverage)")
class WorkshopControllerIntegrationTest {

    @Mock private IWorkshopService workshopService;

    private WorkshopController workshopController;
    private MockMvc mockMvc;

    @BeforeEach
    void setup() {
        workshopController = new WorkshopController(workshopService);
        mockMvc = MockMvcBuilders.standaloneSetup(workshopController).build();
    }

    @Nested
    @DisplayName("1️⃣ Workshop Event Detail Tests")
    class WorkshopEventDetailTests {

        @Test
        @DisplayName("WORKSHOP-001: Khi tìm thấy event, trả về view với eventDetail (200 OK)")
        void whenEventFound_thenReturnViewWithEventDetail() throws Exception {
            Long eventId = 1L;
            WorkshopEventDetailDTO eventDetail = new WorkshopEventDetailDTO();

            when(workshopService.getWorkshopEventById(eventId)).thenReturn(eventDetail);

            mockMvc.perform(get("/workshop/{id}", eventId))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(view().name("workshop/workshopHome"))
                    .andExpect(model().attributeExists("eventDetail"))
                    .andExpect(model().attribute("eventDetail", eventDetail))
                    .andExpect(model().attributeDoesNotExist("error"));
        }

        @Test
        @DisplayName("WORKSHOP-002: Khi không tìm thấy event, service throw Exception, trả về view với error")
        void whenEventNotFound_thenReturnViewWithError() throws Exception {
            Long eventId = 999L;

            when(workshopService.getWorkshopEventById(eventId))
                    .thenThrow(new RuntimeException("Event not found"));

            mockMvc.perform(get("/workshop/{id}", eventId))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(view().name("workshop/workshopHome"))
                    .andExpect(model().attributeDoesNotExist("eventDetail"))
                    .andExpect(model().attributeExists("error"))
                    .andExpect(model().attribute("error", "Không thể tìm thấy workshop bạn yêu cầu."));
        }

        @Test
        @DisplayName("WORKSHOP-003: Khi service throw IllegalArgumentException, trả về view với error")
        void whenServiceThrowsIllegalArgumentException_thenReturnViewWithError() throws Exception {
            Long eventId = 123L;

            when(workshopService.getWorkshopEventById(eventId))
                    .thenThrow(new IllegalArgumentException("Invalid event ID"));

            mockMvc.perform(get("/workshop/{id}", eventId))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(view().name("workshop/workshopHome"))
                    .andExpect(model().attributeDoesNotExist("eventDetail"))
                    .andExpect(model().attributeExists("error"))
                    .andExpect(model().attribute("error", "Không thể tìm thấy workshop bạn yêu cầu."));
        }

        @Test
        @DisplayName("WORKSHOP-004: Khi service throw NullPointerException, trả về view với error")
        void whenServiceThrowsNullPointerException_thenReturnViewWithError() throws Exception {
            Long eventId = 456L;

            when(workshopService.getWorkshopEventById(eventId))
                    .thenThrow(new NullPointerException("Service returned null"));

            mockMvc.perform(get("/workshop/{id}", eventId))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(view().name("workshop/workshopHome"))
                    .andExpect(model().attributeDoesNotExist("eventDetail"))
                    .andExpect(model().attributeExists("error"))
                    .andExpect(model().attribute("error", "Không thể tìm thấy workshop bạn yêu cầu."));
        }

        @Test
        @DisplayName("WORKSHOP-005: Khi eventDetail có đầy đủ thông tin, model chứa tất cả data")
        void whenEventDetailHasFullData_thenModelContainsAllData() throws Exception {
            Long eventId = 789L;
            WorkshopEventDetailDTO eventDetail = new WorkshopEventDetailDTO();

            when(workshopService.getWorkshopEventById(eventId)).thenReturn(eventDetail);

            mockMvc.perform(get("/workshop/{id}", eventId))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(view().name("workshop/workshopHome"))
                    .andExpect(model().attribute("eventDetail", eventDetail))
                    .andExpect(model().attributeDoesNotExist("error"));
        }

        @Test
        @DisplayName("WORKSHOP-006: Khi eventId là 0, service throw exception, trả về error")
        void whenEventIdIsZero_thenReturnError() throws Exception {
            Long eventId = 0L;

            when(workshopService.getWorkshopEventById(eventId))
                    .thenThrow(new RuntimeException("Invalid event ID: 0"));

            mockMvc.perform(get("/workshop/{id}", eventId))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(view().name("workshop/workshopHome"))
                    .andExpect(model().attributeExists("error"))
                    .andExpect(model().attribute("error", "Không thể tìm thấy workshop bạn yêu cầu."));
        }

        @Test
        @DisplayName("WORKSHOP-007: Khi eventId là số âm, service throw exception, trả về error")
        void whenEventIdIsNegative_thenReturnError() throws Exception {
            Long eventId = -1L;

            when(workshopService.getWorkshopEventById(eventId))
                    .thenThrow(new IllegalArgumentException("Event ID cannot be negative"));

            mockMvc.perform(get("/workshop/{id}", eventId))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(view().name("workshop/workshopHome"))
                    .andExpect(model().attributeExists("error"))
                    .andExpect(model().attribute("error", "Không thể tìm thấy workshop bạn yêu cầu."));
        }
    }
}
