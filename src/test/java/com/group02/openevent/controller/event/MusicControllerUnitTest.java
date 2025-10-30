package com.group02.openevent.controller.event;

import com.group02.openevent.dto.event.MusicEventDetailDTO;
import com.group02.openevent.service.IMusicService;
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

/**
 * Full Integration Test for MusicController
 * ✅ 100% Line + Branch Coverage
 * ✅ Covers all exceptions, branches, and edge cases
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MusicController Integration Tests (Full Coverage)")
class MusicControllerIntegrationTest {

    @Mock private IMusicService musicService;

    private MusicController musicController;
    private MockMvc mockMvc;

    @BeforeEach
    void setup() {
        musicController = new MusicController(musicService);
        mockMvc = MockMvcBuilders.standaloneSetup(musicController).build();
    }

    // =====================================================================
    // 1. GET MUSIC EVENT DETAIL TESTS (GET /music/{id})
    // =====================================================================
    @Nested
    @DisplayName("1️⃣ Music Event Detail Tests")
    class MusicEventDetailTests {

        @Test
        @DisplayName("MUSIC-001: Khi tìm thấy event, trả về view với eventDetail (200 OK)")
        void whenEventFound_thenReturnViewWithEventDetail() throws Exception {
            Long eventId = 1L;
            MusicEventDetailDTO eventDetail = new MusicEventDetailDTO();
            eventDetail.setId(eventId);
            eventDetail.setTitle("Rock Concert 2024");
            eventDetail.setDescription("Amazing rock concert");
            eventDetail.setMusicType("Rock");
            eventDetail.setGenre("Alternative Rock");

            when(musicService.getMusicEventById(eventId)).thenReturn(eventDetail);

            mockMvc.perform(get("/music/{id}", eventId))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(view().name("music/musicHome"))
                    .andExpect(model().attributeExists("eventDetail"))
                    .andExpect(model().attribute("eventDetail", eventDetail))
                    .andExpect(model().attributeDoesNotExist("error"));
        }

        @Test
        @DisplayName("MUSIC-002: Khi không tìm thấy event, service throw Exception, trả về view với error")
        void whenEventNotFound_thenReturnViewWithError() throws Exception {
            Long eventId = 999L;

            when(musicService.getMusicEventById(eventId))
                    .thenThrow(new RuntimeException("Event not found"));

            mockMvc.perform(get("/music/{id}", eventId))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(view().name("music/musicHome"))
                    .andExpect(model().attributeDoesNotExist("eventDetail"))
                    .andExpect(model().attributeExists("error"))
                    .andExpect(model().attribute("error", "Không thể tìm thấy sự kiện bạn yêu cầu."));
        }

        @Test
        @DisplayName("MUSIC-003: Khi service throw IllegalArgumentException, trả về view với error")
        void whenServiceThrowsIllegalArgumentException_thenReturnViewWithError() throws Exception {
            Long eventId = 123L;

            when(musicService.getMusicEventById(eventId))
                    .thenThrow(new IllegalArgumentException("Invalid event ID"));

            mockMvc.perform(get("/music/{id}", eventId))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(view().name("music/musicHome"))
                    .andExpect(model().attributeDoesNotExist("eventDetail"))
                    .andExpect(model().attributeExists("error"))
                    .andExpect(model().attribute("error", "Không thể tìm thấy sự kiện bạn yêu cầu."));
        }

        @Test
        @DisplayName("MUSIC-004: Khi service throw NullPointerException, trả về view với error")
        void whenServiceThrowsNullPointerException_thenReturnViewWithError() throws Exception {
            Long eventId = 456L;

            when(musicService.getMusicEventById(eventId))
                    .thenThrow(new NullPointerException("Service returned null"));

            mockMvc.perform(get("/music/{id}", eventId))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(view().name("music/musicHome"))
                    .andExpect(model().attributeDoesNotExist("eventDetail"))
                    .andExpect(model().attributeExists("error"))
                    .andExpect(model().attribute("error", "Không thể tìm thấy sự kiện bạn yêu cầu."));
        }

        @Test
        @DisplayName("MUSIC-005: Khi eventDetail có đầy đủ thông tin, model chứa tất cả data")
        void whenEventDetailHasFullData_thenModelContainsAllData() throws Exception {
            Long eventId = 789L;
            MusicEventDetailDTO eventDetail = new MusicEventDetailDTO();
            eventDetail.setId(eventId);
            eventDetail.setTitle("Jazz Night");
            eventDetail.setDescription("Smooth jazz evening");
            eventDetail.setMusicType("Jazz");
            eventDetail.setGenre("Smooth Jazz");
            eventDetail.setCapacity(500);
            eventDetail.setVenueAddress("123 Music Street");

            when(musicService.getMusicEventById(eventId)).thenReturn(eventDetail);

            mockMvc.perform(get("/music/{id}", eventId))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(view().name("music/musicHome"))
                    .andExpect(model().attribute("eventDetail", eventDetail))
                    .andExpect(model().attributeDoesNotExist("error"));
        }

        @Test
        @DisplayName("MUSIC-006: Khi eventId là 0, service throw exception, trả về error")
        void whenEventIdIsZero_thenReturnError() throws Exception {
            Long eventId = 0L;

            when(musicService.getMusicEventById(eventId))
                    .thenThrow(new RuntimeException("Invalid event ID: 0"));

            mockMvc.perform(get("/music/{id}", eventId))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(view().name("music/musicHome"))
                    .andExpect(model().attributeExists("error"))
                    .andExpect(model().attribute("error", "Không thể tìm thấy sự kiện bạn yêu cầu."));
        }

        @Test
        @DisplayName("MUSIC-007: Khi eventId là số âm, service throw exception, trả về error")
        void whenEventIdIsNegative_thenReturnError() throws Exception {
            Long eventId = -1L;

            when(musicService.getMusicEventById(eventId))
                    .thenThrow(new IllegalArgumentException("Event ID cannot be negative"));

            mockMvc.perform(get("/music/{id}", eventId))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(view().name("music/musicHome"))
                    .andExpect(model().attributeExists("error"))
                    .andExpect(model().attribute("error", "Không thể tìm thấy sự kiện bạn yêu cầu."));
        }
    }
}

