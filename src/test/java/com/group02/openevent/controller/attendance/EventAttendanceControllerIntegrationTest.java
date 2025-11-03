package com.group02.openevent.controller.attendance;

import com.group02.openevent.ai.security.AISecurityService;
import com.group02.openevent.ai.security.RateLimitingService;
import com.group02.openevent.config.SessionInterceptor;
import com.group02.openevent.dto.attendance.AttendanceRequest;
import com.group02.openevent.dto.attendance.AttendanceStatsDTO;
import com.group02.openevent.model.attendance.EventAttendance;
import com.group02.openevent.model.event.Event;
import com.group02.openevent.service.EventAttendanceService;
import com.group02.openevent.service.EventService;
import com.group02.openevent.service.QRCodeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = EventAttendanceController.class,
        excludeAutoConfiguration = SecurityAutoConfiguration.class)
@DisplayName("EventAttendanceController Integration Tests")
class EventAttendanceControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SessionInterceptor sessionInterceptor;
    @MockBean
    private EventService eventService;
    @MockBean
    private EventAttendanceService attendanceService;
    @MockBean
    private QRCodeService qrCodeService;

    private Event event;
    private EventAttendance attendance;
    private AttendanceRequest attendanceRequest;

    private static final Long EVENT_ID = 1L;
    private static final String EMAIL = "test@example.com";
    @MockitoBean
    private RateLimitingService rateLimitingService;

    @MockitoBean
    private AISecurityService aiSecurityService;
    @BeforeEach
    void setUp() throws Exception {
        event = new Event();
        event.setId(EVENT_ID);
        event.setTitle("Test Event");

        attendance = new EventAttendance();
        attendance.setAttendanceId(1L);
        attendance.setEvent(event);
        attendance.setEmail(EMAIL);
        attendance.setFullName("Test User");
        attendance.setStatus(EventAttendance.AttendanceStatus.CHECKED_IN);
        attendance.setCheckInTime(LocalDateTime.now());

        attendanceRequest = new AttendanceRequest();
        attendanceRequest.setEmail(EMAIL);
        attendanceRequest.setFullName("Test User");

        // Mock SessionInterceptor để cho phép tất cả request đi qua
        when(sessionInterceptor.preHandle(any(), any(), any())).thenReturn(true);
    }

    @Nested
    @DisplayName("showAttendancePage Tests")
    class ShowAttendancePageTests {
        @Test
        @DisplayName("TC-01: Show attendance page successfully")
        void showAttendancePage_Success() throws Exception {
            // Arrange
            when(eventService.getEventById(EVENT_ID)).thenReturn(Optional.of(event));

            // Act & Assert
            mockMvc.perform(get("/events/{eventId}/attendance", EVENT_ID))
                    .andExpect(status().isOk())
                    .andExpect(view().name("event/event-checkin-page"))
                    .andExpect(model().attributeExists("event"))
                    .andExpect(model().attributeExists("checkinUrl"))
                    .andExpect(model().attributeExists("checkoutUrl"));
        }

        @Test
        @DisplayName("TC-02: Show attendance page fails when event not found")
        void showAttendancePage_EventNotFound() throws Exception {
            // Arrange
            when(eventService.getEventById(EVENT_ID)).thenReturn(Optional.empty());

            // Act & Assert
            mockMvc.perform(get("/events/{eventId}/attendance", EVENT_ID))
                    .andExpect(status().is4xxClientError());
        }
    }

    @Nested
    @DisplayName("qrCheckinRedirect Tests")
    class QrCheckinRedirectTests {
        @Test
        @DisplayName("TC-03: QR check-in redirect successfully")
        void qrCheckinRedirect_Success() throws Exception {
            // Arrange
            when(eventService.getEventById(EVENT_ID)).thenReturn(Optional.of(event));

            // Act & Assert
            mockMvc.perform(get("/events/{eventId}/qr-checkin", EVENT_ID))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrlPattern("/login?checkin=true&eventId=*&eventTitle=*&action=checkin&redirectUrl=*"));
        }
    }

    @Nested
    @DisplayName("qrCheckoutRedirect Tests")
    class QrCheckoutRedirectTests {
        @Test
        @DisplayName("TC-04: QR check-out redirect successfully")
        void qrCheckoutRedirect_Success() throws Exception {
            // Arrange
            when(eventService.getEventById(EVENT_ID)).thenReturn(Optional.of(event));

            // Act & Assert
            mockMvc.perform(get("/events/{eventId}/qr-checkout", EVENT_ID))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrlPattern("/login?checkin=true&eventId=*&eventTitle=*&action=checkout&redirectUrl=*"));
        }
    }

    @Nested
    @DisplayName("showCheckinForm Tests")
    class ShowCheckinFormTests {
        @Test
        @DisplayName("TC-05: Show check-in form redirects to login when anonymous")
        void showCheckinForm_Anonymous_RedirectsToLogin() throws Exception {
            // Arrange
            when(eventService.getEventById(EVENT_ID)).thenReturn(Optional.of(event));

            // Act & Assert - Without authentication, should redirect to login
            mockMvc.perform(get("/events/{eventId}/checkin-form", EVENT_ID))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrlPattern("/login?redirect=*"));
        }
    }

    @Nested
    @DisplayName("processCheckin Tests")
    class ProcessCheckinTests {
        @Test
        @DisplayName("TC-07: Process check-in successfully")
        void processCheckin_Success() throws Exception {
            // Arrange
            when(attendanceService.checkIn(eq(EVENT_ID), any(AttendanceRequest.class)))
                    .thenReturn(attendance);

            // Act & Assert
            mockMvc.perform(post("/events/{eventId}/checkin", EVENT_ID)
                            .param("email", EMAIL)
                            .param("fullName", "Test User")
                            .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrlPattern("/events/" + EVENT_ID + "/checkin-form*"))
                    .andExpect(flash().attributeExists("successMessage"))
                    .andExpect(flash().attributeExists("checkInTime"));
        }

        @Test
        @DisplayName("TC-08: Process check-in fails when no paid order")
        void processCheckin_NoPaidOrder() throws Exception {
            // Arrange
            when(attendanceService.checkIn(eq(EVENT_ID), any(AttendanceRequest.class)))
                    .thenThrow(new RuntimeException("Bạn không đăng ký sự kiện này"));

            // Act & Assert
            mockMvc.perform(post("/events/{eventId}/checkin", EVENT_ID)
                            .param("email", EMAIL)
                            .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrlPattern("/events/" + EVENT_ID + "/checkin-form*"))
                    .andExpect(flash().attributeExists("errorMessage"));
        }
    }

    @Nested
    @DisplayName("showCheckoutForm Tests")
    class ShowCheckoutFormTests {
        @Test
        @DisplayName("TC-09: Show check-out form successfully")
        void showCheckoutForm_Success() throws Exception {
            // Arrange
            when(eventService.getEventById(EVENT_ID)).thenReturn(Optional.of(event));

            // Act & Assert
            mockMvc.perform(get("/events/{eventId}/checkout-form", EVENT_ID))
                    .andExpect(status().isOk())
                    .andExpect(view().name("event/checkout-form"))
                    .andExpect(model().attributeExists("event"));
        }
    }

    @Nested
    @DisplayName("processCheckout Tests")
    class ProcessCheckoutTests {
        @Test
        @DisplayName("TC-10: Process check-out successfully")
        void processCheckout_Success() throws Exception {
            // Arrange
            attendance.setCheckOutTime(LocalDateTime.now());
            attendance.setStatus(EventAttendance.AttendanceStatus.CHECKED_OUT);
            when(attendanceService.checkOut(EVENT_ID, EMAIL)).thenReturn(attendance);

            // Act & Assert
            mockMvc.perform(post("/events/{eventId}/checkout", EVENT_ID)
                            .param("email", EMAIL)
                            .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrlPattern("/events/" + EVENT_ID + "/checkout-form*"))
                    .andExpect(flash().attributeExists("successMessage"));
        }

        @Test
        @DisplayName("TC-11: Process check-out fails when not checked in")
        void processCheckout_NotCheckedIn() throws Exception {
            // Arrange
            when(attendanceService.checkOut(EVENT_ID, EMAIL))
                    .thenThrow(new RuntimeException("Bạn chưa check-in"));

            // Act & Assert
            mockMvc.perform(post("/events/{eventId}/checkout", EVENT_ID)
                            .param("email", EMAIL)
                            .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrlPattern("/events/" + EVENT_ID + "/checkout-form*"))
                    .andExpect(flash().attributeExists("errorMessage"));
        }

        @Test
        @DisplayName("TC-12: Process check-out fails when no paid order")
        void processCheckout_NoPaidOrder() throws Exception {
            // Arrange
            when(attendanceService.checkOut(EVENT_ID, EMAIL))
                    .thenThrow(new RuntimeException("Bạn không đăng ký sự kiện này"));

            // Act & Assert
            mockMvc.perform(post("/events/{eventId}/checkout", EVENT_ID)
                            .param("email", EMAIL)
                            .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(flash().attributeExists("errorMessage"));
        }
    }

    @Nested
    @DisplayName("getAttendances Tests")
    class GetAttendancesTests {
        @Test
        @DisplayName("TC-13: Get attendances successfully")
        void getAttendances_Success() throws Exception {
            // Arrange
            when(attendanceService.getAttendancesByEventId(EVENT_ID))
                    .thenReturn(List.of(attendance));

            // Act & Assert
            mockMvc.perform(get("/events/{eventId}/attendances", EVENT_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$[0].email").value(EMAIL));
        }
    }

    @Nested
    @DisplayName("getAttendanceStats Tests")
    class GetAttendanceStatsTests {
        @Test
        @DisplayName("TC-14: Get attendance stats successfully")
        void getAttendanceStats_Success() throws Exception {
            // Arrange
            AttendanceStatsDTO stats = new AttendanceStatsDTO(EVENT_ID, "Test Event", 10L, 5L, 5L, 10L);
            when(attendanceService.getAttendanceStats(EVENT_ID)).thenReturn(stats);

            // Act & Assert
            mockMvc.perform(get("/events/{eventId}/attendance-stats", EVENT_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.eventId").value(EVENT_ID))
                    .andExpect(jsonPath("$.totalCheckedIn").value(10))
                    .andExpect(jsonPath("$.totalCheckedOut").value(5));
        }
    }

    @Nested
    @DisplayName("generateQRCode Tests")
    class GenerateQRCodeTests {
        @Test
        @DisplayName("TC-15: Generate QR code successfully")
        void generateQRCode_Success() throws Exception {
            // Arrange
            String url = "https://example.com";
            byte[] qrImage = new byte[]{1, 2, 3};
            when(qrCodeService.generateQRCodeImage(url, 350, 350)).thenReturn(qrImage);

            // Act & Assert
            mockMvc.perform(get("/events/qr-code/generate").param("url", url))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.IMAGE_PNG));
        }

        @Test
        @DisplayName("TC-16: Generate QR code fails when service throws exception")
        void generateQRCode_Failure() throws Exception {
            // Arrange
            String url = "https://example.com";
            when(qrCodeService.generateQRCodeImage(url, 350, 350))
                    .thenThrow(new RuntimeException("QR generation failed"));

            // Act & Assert
            mockMvc.perform(get("/events/qr-code/generate").param("url", url))
                    .andExpect(status().is4xxClientError());
        }
    }
}

