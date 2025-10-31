package com.group02.openevent.service.impl;

import com.group02.openevent.dto.attendance.AttendanceRequest;
import com.group02.openevent.dto.attendance.AttendanceStatsDTO;
import com.group02.openevent.model.attendance.EventAttendance;
import com.group02.openevent.model.event.Event;
import com.group02.openevent.repository.IEventAttendanceRepo;
import com.group02.openevent.repository.IEventRepo;
import com.group02.openevent.repository.IOrderRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("EventAttendanceServiceImpl Unit Tests")
class EventAttendanceServiceImplTest {

    @InjectMocks
    private EventAttendanceServiceImpl attendanceService;

    @Mock
    private IEventAttendanceRepo attendanceRepo;
    @Mock
    private IEventRepo eventRepo;
    @Mock
    private IOrderRepo orderRepo;

    private Event event;
    private AttendanceRequest attendanceRequest;
    private EventAttendance existingAttendance;

    private static final Long EVENT_ID = 1L;
    private static final String EMAIL = "test@example.com";
    private static final String NORMALIZED_EMAIL = "test@example.com";

    @BeforeEach
    void setUp() {
        event = new Event();
        event.setId(EVENT_ID);
        event.setTitle("Test Event");

        attendanceRequest = new AttendanceRequest();
        attendanceRequest.setEmail(EMAIL);
        attendanceRequest.setFullName("Test User");
        attendanceRequest.setPhone("1234567890");
        attendanceRequest.setOrganization("Test Org");

        existingAttendance = new EventAttendance();
        existingAttendance.setEvent(event);
        existingAttendance.setEmail(NORMALIZED_EMAIL);
        existingAttendance.setStatus(EventAttendance.AttendanceStatus.CHECKED_IN);
        existingAttendance.setCheckInTime(LocalDateTime.now());
    }

    @Nested
    @DisplayName("checkIn Tests")
    class CheckInTests {
        @Test
        @DisplayName("TC-01: Check-in successfully for new attendance")
        void checkIn_NewAttendance_Success() {
            // Arrange
            when(eventRepo.findById(EVENT_ID)).thenReturn(Optional.of(event));
            when(orderRepo.existsPaidByEventIdAndParticipantEmail(EVENT_ID, NORMALIZED_EMAIL)).thenReturn(true);
            when(attendanceRepo.findByEventIdAndEmail(EVENT_ID, NORMALIZED_EMAIL)).thenReturn(Optional.empty());
            when(attendanceRepo.save(any(EventAttendance.class))).thenAnswer(invocation -> {
                EventAttendance att = invocation.getArgument(0);
                att.setAttendanceId(1L);
                return att;
            });

            // Act
            EventAttendance result = attendanceService.checkIn(EVENT_ID, attendanceRequest);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(EventAttendance.AttendanceStatus.CHECKED_IN);
            assertThat(result.getEmail()).isEqualTo(NORMALIZED_EMAIL);
            assertThat(result.getCheckInTime()).isNotNull();
            verify(attendanceRepo).save(any(EventAttendance.class));
        }

        @Test
        @DisplayName("TC-02: Check-in fails when event not found")
        void checkIn_EventNotFound() {
            // Arrange
            when(eventRepo.findById(EVENT_ID)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> attendanceService.checkIn(EVENT_ID, attendanceRequest))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Event not found");
        }

        @Test
        @DisplayName("TC-03: Check-in fails when no paid order exists")
        void checkIn_NoPaidOrder() {
            // Arrange
            when(eventRepo.findById(EVENT_ID)).thenReturn(Optional.of(event));
            when(orderRepo.existsPaidByEventIdAndParticipantEmail(EVENT_ID, NORMALIZED_EMAIL)).thenReturn(false);

            // Act & Assert
            assertThatThrownBy(() -> attendanceService.checkIn(EVENT_ID, attendanceRequest))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("không tìm thấy vé đã thanh toán");
        }

        @Test
        @DisplayName("TC-04: Check-in fails when email is null")
        void checkIn_NullEmail() {
            // Arrange
            attendanceRequest.setEmail(null);
            when(eventRepo.findById(EVENT_ID)).thenReturn(Optional.of(event));

            // Act & Assert
            assertThatThrownBy(() -> attendanceService.checkIn(EVENT_ID, attendanceRequest))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("không tìm thấy vé đã thanh toán");
        }

        @Test
        @DisplayName("TC-05: Check-in updates existing PENDING attendance")
        void checkIn_UpdatePendingAttendance() {
            // Arrange
            existingAttendance.setStatus(EventAttendance.AttendanceStatus.PENDING);
            existingAttendance.setCheckInTime(null);

            when(eventRepo.findById(EVENT_ID)).thenReturn(Optional.of(event));
            when(orderRepo.existsPaidByEventIdAndParticipantEmail(EVENT_ID, NORMALIZED_EMAIL)).thenReturn(true);
            when(attendanceRepo.findByEventIdAndEmail(EVENT_ID, NORMALIZED_EMAIL))
                    .thenReturn(Optional.of(existingAttendance));
            when(attendanceRepo.save(any(EventAttendance.class))).thenReturn(existingAttendance);

            // Act
            EventAttendance result = attendanceService.checkIn(EVENT_ID, attendanceRequest);

            // Assert
            assertThat(result.getStatus()).isEqualTo(EventAttendance.AttendanceStatus.CHECKED_IN);
            assertThat(result.getCheckInTime()).isNotNull();
            verify(attendanceRepo).save(existingAttendance);
        }

        @Test
        @DisplayName("TC-06: Check-in fails when already checked in")
        void checkIn_AlreadyCheckedIn() {
            // Arrange
            existingAttendance.setStatus(EventAttendance.AttendanceStatus.CHECKED_IN);
            existingAttendance.setCheckInTime(LocalDateTime.now().minusHours(1));

            when(eventRepo.findById(EVENT_ID)).thenReturn(Optional.of(event));
            when(orderRepo.existsPaidByEventIdAndParticipantEmail(EVENT_ID, NORMALIZED_EMAIL)).thenReturn(true);
            when(attendanceRepo.findByEventIdAndEmail(EVENT_ID, NORMALIZED_EMAIL))
                    .thenReturn(Optional.of(existingAttendance));

            // Act & Assert
            assertThatThrownBy(() -> attendanceService.checkIn(EVENT_ID, attendanceRequest))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("đã check-in");
        }

        @Test
        @DisplayName("TC-07: Check-in normalizes email to lowercase")
        void checkIn_NormalizesEmail() {
            // Arrange
            attendanceRequest.setEmail("Test@EXAMPLE.com");
            String normalized = "test@example.com";

            when(eventRepo.findById(EVENT_ID)).thenReturn(Optional.of(event));
            when(orderRepo.existsPaidByEventIdAndParticipantEmail(EVENT_ID, normalized)).thenReturn(true);
            when(attendanceRepo.findByEventIdAndEmail(EVENT_ID, normalized)).thenReturn(Optional.empty());
            when(attendanceRepo.save(any(EventAttendance.class))).thenAnswer(invocation -> {
                EventAttendance att = invocation.getArgument(0);
                att.setAttendanceId(1L);
                return att;
            });

            // Act
            EventAttendance result = attendanceService.checkIn(EVENT_ID, attendanceRequest);

            // Assert
            assertThat(result.getEmail()).isEqualTo(normalized);
            verify(orderRepo).existsPaidByEventIdAndParticipantEmail(EVENT_ID, normalized);
        }
    }

    @Nested
    @DisplayName("checkOut Tests")
    class CheckOutTests {
        @Test
        @DisplayName("TC-08: Check-out successfully")
        void checkOut_Success() {
            // Arrange
            existingAttendance.setStatus(EventAttendance.AttendanceStatus.CHECKED_IN);
            existingAttendance.setCheckInTime(LocalDateTime.now().minusHours(1));
            existingAttendance.setCheckOutTime(null);

            when(attendanceRepo.findByEventIdAndEmail(EVENT_ID, NORMALIZED_EMAIL))
                    .thenReturn(Optional.of(existingAttendance));
            when(orderRepo.existsPaidByEventIdAndParticipantEmail(EVENT_ID, NORMALIZED_EMAIL)).thenReturn(true);
            when(attendanceRepo.save(any(EventAttendance.class))).thenReturn(existingAttendance);

            // Act
            EventAttendance result = attendanceService.checkOut(EVENT_ID, EMAIL);

            // Assert
            assertThat(result.getStatus()).isEqualTo(EventAttendance.AttendanceStatus.CHECKED_OUT);
            assertThat(result.getCheckOutTime()).isNotNull();
            verify(attendanceRepo).save(existingAttendance);
        }

        @Test
        @DisplayName("TC-09: Check-out fails when attendance not found")
        void checkOut_AttendanceNotFound() {
            // Arrange
            when(attendanceRepo.findByEventIdAndEmail(EVENT_ID, NORMALIZED_EMAIL))
                    .thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> attendanceService.checkOut(EVENT_ID, EMAIL))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Không tìm thấy thông tin check-in");
        }

        @Test
        @DisplayName("TC-10: Check-out fails when no paid order exists")
        void checkOut_NoPaidOrder() {
            // Arrange
            when(attendanceRepo.findByEventIdAndEmail(EVENT_ID, NORMALIZED_EMAIL))
                    .thenReturn(Optional.of(existingAttendance));
            when(orderRepo.existsPaidByEventIdAndParticipantEmail(EVENT_ID, NORMALIZED_EMAIL)).thenReturn(false);

            // Act & Assert
            assertThatThrownBy(() -> attendanceService.checkOut(EVENT_ID, EMAIL))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("không tìm thấy vé đã thanh toán");
        }

        @Test
        @DisplayName("TC-11: Check-out fails when not checked in")
        void checkOut_NotCheckedIn() {
            // Arrange
            existingAttendance.setStatus(EventAttendance.AttendanceStatus.PENDING);

            when(attendanceRepo.findByEventIdAndEmail(EVENT_ID, NORMALIZED_EMAIL))
                    .thenReturn(Optional.of(existingAttendance));
            when(orderRepo.existsPaidByEventIdAndParticipantEmail(EVENT_ID, NORMALIZED_EMAIL)).thenReturn(true);

            // Act & Assert
            assertThatThrownBy(() -> attendanceService.checkOut(EVENT_ID, EMAIL))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("chưa check-in");
        }

        @Test
        @DisplayName("TC-12: Check-out fails when already checked out")
        void checkOut_AlreadyCheckedOut() {
            // Arrange
            existingAttendance.setStatus(EventAttendance.AttendanceStatus.CHECKED_OUT);
            existingAttendance.setCheckOutTime(LocalDateTime.now().minusMinutes(30));

            when(attendanceRepo.findByEventIdAndEmail(EVENT_ID, NORMALIZED_EMAIL))
                    .thenReturn(Optional.of(existingAttendance));
            when(orderRepo.existsPaidByEventIdAndParticipantEmail(EVENT_ID, NORMALIZED_EMAIL)).thenReturn(true);

            // Act & Assert
            assertThatThrownBy(() -> attendanceService.checkOut(EVENT_ID, EMAIL))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("chưa check-in hoặc đã check-out");
        }

        @Test
        @DisplayName("TC-13: Check-out normalizes email")
        void checkOut_NormalizesEmail() {
            // Arrange
            String mixedCaseEmail = "Test@EXAMPLE.com";
            String normalized = "test@example.com";
            existingAttendance.setEmail(normalized);
            existingAttendance.setStatus(EventAttendance.AttendanceStatus.CHECKED_IN);

            when(attendanceRepo.findByEventIdAndEmail(EVENT_ID, normalized))
                    .thenReturn(Optional.of(existingAttendance));
            when(orderRepo.existsPaidByEventIdAndParticipantEmail(EVENT_ID, normalized)).thenReturn(true);
            when(attendanceRepo.save(any(EventAttendance.class))).thenReturn(existingAttendance);

            // Act
            attendanceService.checkOut(EVENT_ID, mixedCaseEmail);

            // Assert
            verify(attendanceRepo).findByEventIdAndEmail(EVENT_ID, normalized);
            verify(orderRepo).existsPaidByEventIdAndParticipantEmail(EVENT_ID, normalized);
        }
    }

    @Nested
    @DisplayName("getAttendancesByEventId Tests")
    class GetAttendancesByEventIdTests {
        @Test
        @DisplayName("TC-14: Get attendances by event ID successfully")
        void getAttendancesByEventId_Success() {
            // Arrange
            when(attendanceRepo.findByEventId(EVENT_ID)).thenReturn(List.of(existingAttendance));

            // Act
            List<EventAttendance> result = attendanceService.getAttendancesByEventId(EVENT_ID);

            // Assert
            assertThat(result).hasSize(1);
            assertThat(result.get(0)).isEqualTo(existingAttendance);
            verify(attendanceRepo).findByEventId(EVENT_ID);
        }
    }

    @Nested
    @DisplayName("getAttendanceByEventAndEmail Tests")
    class GetAttendanceByEventAndEmailTests {
        @Test
        @DisplayName("TC-15: Get attendance by event and email successfully")
        void getAttendanceByEventAndEmail_Success() {
            // Arrange
            when(attendanceRepo.findByEventIdAndEmail(EVENT_ID, EMAIL))
                    .thenReturn(Optional.of(existingAttendance));

            // Act
            Optional<EventAttendance> result = attendanceService.getAttendanceByEventAndEmail(EVENT_ID, EMAIL);

            // Assert
            assertThat(result).isPresent();
            assertThat(result.get()).isEqualTo(existingAttendance);
        }

        @Test
        @DisplayName("TC-16: Get attendance returns empty when not found")
        void getAttendanceByEventAndEmail_NotFound() {
            // Arrange
            when(attendanceRepo.findByEventIdAndEmail(EVENT_ID, EMAIL))
                    .thenReturn(Optional.empty());

            // Act
            Optional<EventAttendance> result = attendanceService.getAttendanceByEventAndEmail(EVENT_ID, EMAIL);

            // Assert
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("getAttendanceStats Tests")
    class GetAttendanceStatsTests {
        @Test
        @DisplayName("TC-17: Get attendance stats successfully")
        void getAttendanceStats_Success() {
            // Arrange
            when(eventRepo.findById(EVENT_ID)).thenReturn(Optional.of(event));
            when(attendanceRepo.countCheckedInByEventId(EVENT_ID)).thenReturn(10L);
            when(attendanceRepo.countCheckedOutByEventId(EVENT_ID)).thenReturn(5L);
            when(attendanceRepo.countCurrentlyPresentByEventId(EVENT_ID)).thenReturn(5L);
            when(attendanceRepo.countByEventId(EVENT_ID)).thenReturn(10L);

            // Act
            AttendanceStatsDTO result = attendanceService.getAttendanceStats(EVENT_ID);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getEventId()).isEqualTo(EVENT_ID);
            assertThat(result.getEventTitle()).isEqualTo("Test Event");
            assertThat(result.getTotalCheckedIn()).isEqualTo(10L);
            assertThat(result.getTotalCheckedOut()).isEqualTo(5L);
            assertThat(result.getCurrentlyPresent()).isEqualTo(5L);
            assertThat(result.getTotalAttendees()).isEqualTo(10L);
        }

        @Test
        @DisplayName("TC-18: Get attendance stats fails when event not found")
        void getAttendanceStats_EventNotFound() {
            // Arrange
            when(eventRepo.findById(EVENT_ID)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> attendanceService.getAttendanceStats(EVENT_ID))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Event not found");
        }
    }

    @Nested
    @DisplayName("isAlreadyCheckedIn Tests")
    class IsAlreadyCheckedInTests {
        @Test
        @DisplayName("TC-19: Check if already checked in returns true")
        void isAlreadyCheckedIn_True() {
            // Arrange
            when(attendanceRepo.existsByEventIdAndEmailAndCheckedIn(EVENT_ID, EMAIL)).thenReturn(true);

            // Act
            boolean result = attendanceService.isAlreadyCheckedIn(EVENT_ID, EMAIL);

            // Assert
            assertThat(result).isTrue();
            verify(attendanceRepo).existsByEventIdAndEmailAndCheckedIn(EVENT_ID, EMAIL);
        }

        @Test
        @DisplayName("TC-20: Check if already checked in returns false")
        void isAlreadyCheckedIn_False() {
            // Arrange
            when(attendanceRepo.existsByEventIdAndEmailAndCheckedIn(EVENT_ID, EMAIL)).thenReturn(false);

            // Act
            boolean result = attendanceService.isAlreadyCheckedIn(EVENT_ID, EMAIL);

            // Assert
            assertThat(result).isFalse();
            verify(attendanceRepo).existsByEventIdAndEmailAndCheckedIn(EVENT_ID, EMAIL);
        }
    }
}

