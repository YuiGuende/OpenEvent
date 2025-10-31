package com.group02.openevent.service;

import com.group02.openevent.dto.home.EventCardDTO;
import com.group02.openevent.dto.request.PlaceUpdateRequest;
import com.group02.openevent.dto.request.create.EventCreationRequest;
import com.group02.openevent.dto.request.update.EventUpdateRequest;
import com.group02.openevent.dto.response.EventResponse;
import com.group02.openevent.mapper.EventMapper;
import com.group02.openevent.model.enums.EventStatus;
import com.group02.openevent.model.enums.EventType;
import com.group02.openevent.model.event.*;
import com.group02.openevent.model.organization.Organization;
import com.group02.openevent.model.user.Host;
import com.group02.openevent.repository.IEventRepo;
import com.group02.openevent.repository.IHostRepo;
import com.group02.openevent.repository.IOrganizationRepo;
import com.group02.openevent.repository.IPlaceRepo;
import com.group02.openevent.service.impl.EventServiceImpl;
import com.group02.openevent.service.impl.OrderServiceImpl;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;


import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class EventServiceTest {

    @Mock
    OrderService orderService;
    @Mock
    IEventRepo eventRepo;
    @Mock
    EventMapper eventMapper;
    @Mock
    IOrganizationRepo organizationRepo;
    @Mock
    IHostRepo hostRepo;
    @Mock
    IPlaceRepo placeRepo;
    @InjectMocks
    private EventServiceImpl eventService;
    private Event mockEvent;
    private Place mockPlace;
    private Host mockHost;
    private Organization mockOrg;
    private Pageable pageable;

    @BeforeEach
    void setUp() {
        mockOrg = new Organization();
        mockOrg.setOrgName("Test Org");
        mockHost = new Host();
        mockPlace = new Place();
        mockPlace.setPlaceName("Test Place");

        mockEvent = new Event();
        mockEvent.setId(1L);
        mockEvent.setTitle("Test Event");
        mockEvent.setEventType(EventType.MUSIC);
        mockEvent.setStatus(EventStatus.DRAFT);
        mockEvent.setOrganization(null);
        mockEvent.setHost(null);
        mockEvent.setPlaces(new ArrayList<>());

        pageable = (Pageable) PageRequest.of(0, 10);

        // Cài đặt mock chung cho convertToDTO (được gọi bởi nhiều hàm)
        // Dùng lenient() để tránh lỗi "Unnecessary stubbing"
        lenient().when(orderService.countUniqueParticipantsByEventId(anyLong())).thenReturn(0);
    }
    @Test
    void TC01_ShouldCreateWorkshopEvent_WhenEventTypeIsWORKSHOP() {
        // Given
        EventCreationRequest req = new EventCreationRequest();
        req.setEventType(EventType.WORKSHOP);
        Host mockHost = new Host();
        when(hostRepo.getHostById(1L)).thenReturn(mockHost);

        WorkshopEvent mockEvent = new WorkshopEvent();
        EventResponse mockResponse = new EventResponse();
        when(eventRepo.save(any(Event.class))).thenReturn(mockEvent);
        when(eventMapper.toEventResponse(mockEvent)).thenReturn(mockResponse);

        // When
        EventResponse result = eventService.saveEvent(req);

        // Then
        assertNotNull(result);
        verify(eventRepo, times(1)).save(any(WorkshopEvent.class));
        verify(eventMapper, times(1)).createEventFromRequest(eq(req), any(WorkshopEvent.class));
    }
    @Test
    void TC02_ShouldCreateMusicEvent_WhenEventTypeIsMUSIC() {
        EventCreationRequest req = new EventCreationRequest();
        req.setEventType(EventType.MUSIC);
        when(hostRepo.getHostById(1L)).thenReturn(new Host());
        when(eventRepo.save(any(Event.class))).thenReturn(new MusicEvent());
        when(eventMapper.toEventResponse(any())).thenReturn(new EventResponse());

        EventResponse res = eventService.saveEvent(req);

        assertNotNull(res);
        verify(eventRepo).save(any(MusicEvent.class));
    }
    @Test
    void TC03_ShouldHandleUnknownEventType_AndCreateGenericEvent() {
        EventCreationRequest req = new EventCreationRequest();
        req.setEventType(null);
        when(hostRepo.getHostById(1L)).thenReturn(new Host());
        when(eventRepo.save(any(Event.class))).thenReturn(new Event());
        when(eventMapper.toEventResponse(any())).thenReturn(new EventResponse());

        EventResponse res = eventService.saveEvent(req);

        assertNotNull(res);
        verify(eventRepo).save(any(Event.class)); // generic event
    }
    @Test
    void TC05_ShouldThrowException_WhenMapperFails() {
        EventCreationRequest req = new EventCreationRequest();
        req.setEventType(EventType.MUSIC);
        doThrow(RuntimeException.class).when(eventMapper).createEventFromRequest(any(), any());
        assertThrows(RuntimeException.class, () -> eventService.saveEvent(req));
    }
    @Test
    void TC01_ShouldUpdateBasicFields_WhenEventExists() {
        // GIVEN
        Event existing = new MusicEvent();
        existing.setId(1L);

        EventUpdateRequest request = new EventUpdateRequest();
        request.setEventType(EventType.MUSIC);

        when(eventRepo.findById(1L)).thenReturn(Optional.of(existing));
        when(eventRepo.saveAndFlush(any(Event.class))).thenAnswer(i -> i.getArgument(0));
        when(eventMapper.toEventResponse(any(Event.class))).thenReturn(new EventResponse());

        // WHEN
        EventResponse response = eventService.updateEvent(1L, request);

        // THEN
        verify(eventMapper).updateEventFromRequest(request, existing);
        verify(eventRepo).saveAndFlush(existing);
        assertNotNull(response);
    }
    @Test
    void TC02_ShouldUpdateMusicEventFields() {
        // GIVEN
        MusicEvent existing = new MusicEvent();
        existing.setId(2L);
        existing.setMusicType("Old");

        EventUpdateRequest request = new EventUpdateRequest();
        request.setEventType(EventType.MUSIC);
        request.setMusicType("Rock");
        request.setGenre("Indie");
        request.setPerformerCount(5);

        when(eventRepo.findById(2L)).thenReturn(Optional.of(existing));
        when(eventRepo.saveAndFlush(any(Event.class))).thenReturn(existing);
        when(eventMapper.toEventResponse(any())).thenReturn(new EventResponse());

        // WHEN
        eventService.updateEvent(2L, request);

        // THEN
        assertEquals("Rock", existing.getMusicType());
        assertEquals("Indie", existing.getGenre());
        assertEquals(5, existing.getPerformerCount());
    }
    @Test
    void TC03_ShouldUpdateOrganizationAndHost_WhenProvided() {
        // GIVEN
        Event existing = new WorkshopEvent();
        existing.setId(3L);
        EventUpdateRequest request = new EventUpdateRequest();
        request.setOrganizationId(10L);
        request.setHostId(20L);

        Organization org = new Organization();
        org.setOrgId(10L);
        Host host = new Host();
        host.setId(20L);

        when(eventRepo.findById(3L)).thenReturn(Optional.of(existing));
        when(organizationRepo.findById(10L)).thenReturn(Optional.of(org));
        when(hostRepo.findById(20L)).thenReturn(Optional.of(host));
        when(eventRepo.saveAndFlush(any())).thenReturn(existing);
        when(eventMapper.toEventResponse(any())).thenReturn(new EventResponse());

        // WHEN
        eventService.updateEvent(3L, request);

        // THEN
        assertEquals(org, existing.getOrganization());
        assertEquals(host, existing.getHost());
    }
    @Test
    void TC04_ShouldUpdatePlaces_WhenPlaceRequestsProvided() {
        // GIVEN
        Event existing = new Event();
        existing.setId(4L);
        existing.setPlaces(new ArrayList<>());

        PlaceUpdateRequest r1 = new PlaceUpdateRequest(1L, "Hall A", "B1", false);
        PlaceUpdateRequest r2 = new PlaceUpdateRequest(null, "Hall B", "B2", false);
        PlaceUpdateRequest r3 = spy(new PlaceUpdateRequest(2L, "Old Hall", "C1", true));
        when(r3.getIsDeleted()).thenReturn(true);

        EventUpdateRequest request = new EventUpdateRequest();
        request.setPlaceUpdateRequests(List.of(r1, r2, r3));

        when(eventRepo.findById(4L)).thenReturn(Optional.of(existing));
        when(placeRepo.findById(1L)).thenReturn(Optional.of(new Place(1L, "Old A", "B0")));
        lenient().when(placeRepo.save(any(Place.class))).thenAnswer(i -> i.getArgument(0));
        lenient().when(eventRepo.saveAndFlush(any())).thenReturn(existing);

        // WHEN
        eventService.updateEvent(4L, request);

        // THEN
        assertEquals(2, existing.getPlaces().size()); // r3 bị skip
    }
    @Test
    void TC05_ShouldThrowException_WhenEventNotFound() {
        // GIVEN
        when(eventRepo.findById(999L)).thenReturn(Optional.empty());

        // WHEN + THEN
        assertThrows(EntityNotFoundException.class,
                () -> eventService.updateEvent(999L, new EventUpdateRequest()));
    }
    @Test
    void TC01_ShouldUpdateStatusSuccessfully_WhenEventExists() {
        // GIVEN
        Event event = new Event();
        event.setId(10L);
        event.setStatus(EventStatus.DRAFT);

        when(eventRepo.findById(10L)).thenReturn(Optional.of(event));
        when(eventRepo.save(any(Event.class))).thenAnswer(inv -> inv.getArgument(0));

        // WHEN
        Event updated = eventService.updateEventStatus(10L, EventStatus.PUBLIC);

        // THEN
        assertNotNull(updated);
        assertEquals(EventStatus.PUBLIC, updated.getStatus());
        verify(eventRepo).save(event);
    }

    // ✅ TC02: Event không tồn tại
    @Test
    void TC02_ShouldThrowException_WhenEventNotFound() {
        when(eventRepo.findById(99L)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class, () -> {
            eventService.updateEventStatus(99L, EventStatus.CANCEL);
        });

        assertEquals("Event not found with id: 99", ex.getMessage());
        verify(eventRepo, never()).save(any());
    }

    // ✅ TC03: Đảm bảo gán status mới đúng
    @Test
    void TC03_ShouldAssignNewStatusCorrectly() {
        Event event = new Event();
        event.setId(20L);
        event.setStatus(EventStatus.DRAFT);

        when(eventRepo.findById(20L)).thenReturn(Optional.of(event));
        when(eventRepo.save(any(Event.class))).thenAnswer(inv -> inv.getArgument(0));

        Event result = eventService.updateEventStatus(20L, EventStatus.CANCEL);

        assertEquals(EventStatus.CANCEL, result.getStatus());
        verify(eventRepo).save(event);
    }
    @Test
    @DisplayName("Test getCustomerEvents - Should return DTO list")
    void testGetCustomerEvents() {
        // Given
        when(orderService.findConfirmedEventsByCustomerId(1L)).thenReturn(List.of(mockEvent));
        when(orderService.countUniqueParticipantsByEventId(1L)).thenReturn(5); // Cần cho convertToDTO

        // When
        List<EventCardDTO> result = eventService.getCustomerEvents(1L);

        // Then
        assertEquals(1, result.size());
        assertEquals("Test Event", result.get(0).getTitle());
        assertEquals(5, result.get(0).getRegistered()); // Xác minh convertToDTO đã gọi orderService
        verify(orderService, times(1)).findConfirmedEventsByCustomerId(1L);
    }

    @Test
    @DisplayName("Test getLiveEvents - Should call repo with ONGOING status")
    void testGetLiveEvents() {
        // Given
        PageRequest expectedPageable = PageRequest.of(0, 5);
        when(eventRepo.findRecommendedEvents(EventStatus.ONGOING, expectedPageable)).thenReturn(List.of(mockEvent));

        // When
        List<EventCardDTO> result = eventService.getLiveEvents(5);

        // Then
        assertEquals(1, result.size());
        verify(eventRepo, times(1)).findRecommendedEvents(EventStatus.ONGOING, expectedPageable);
    }

    @Test
    @DisplayName("Test getEventResponseById - Should return Event when found")
    void testGetEventResponseById_Found() {
        // Given
        when(eventRepo.findById(1L)).thenReturn(Optional.of(mockEvent));

        // When
        Event result = eventService.getEventResponseById(1L);

        // Then
        assertNotNull(result);
        assertEquals("Test Event", result.getTitle());
    }

    @Test
    @DisplayName("Test getEventResponseById - Should throw RuntimeException when not found")
    void testGetEventResponseById_NotFound() {
        // Given
        when(eventRepo.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            eventService.getEventResponseById(999L);
        });
        assertEquals("Không tìm thấy sự kiện với ID: 999", exception.getMessage());
    }

    // --- Tests for listEvents (4 branches) ---
    @Test
    @DisplayName("Test listEvents - Branch 1: EventType and Status provided")
    void testListEvents_Branch1() {
        eventService.listEvents(EventType.MUSIC, EventStatus.PUBLIC, pageable);
        verify(eventRepo, times(1)).findByEventTypeAndStatus(EventType.MUSIC, EventStatus.PUBLIC, pageable);
    }

    @Test
    @DisplayName("Test listEvents - Branch 2: Only EventType provided")
    void testListEvents_Branch2() {
        eventService.listEvents(EventType.MUSIC, null, pageable);
        verify(eventRepo, times(1)).findByEventType(EventType.MUSIC, pageable);
    }

    @Test
    @DisplayName("Test listEvents - Branch 3: Only Status provided")
    void testListEvents_Branch3() {
        eventService.listEvents(null, EventStatus.PUBLIC, pageable);
        verify(eventRepo, times(1)).findByStatus(EventStatus.PUBLIC, pageable);
    }

    @Test
    @DisplayName("Test listEvents - Branch 4: All null")
    void testListEvents_Branch4() {
        eventService.listEvents(null, null, pageable);
        verify(eventRepo, times(1)).findAll(pageable);
    }

    // --- Tests for getEventsByDepartment (4 branches) ---
    @Test
    @DisplayName("Test getEventsByDepartment - Branch 1: All params provided")
    void testGetEventsByDepartment_Branch1() {
        eventService.getEventsByDepartment(1L, EventType.MUSIC, EventStatus.PUBLIC, pageable);
        verify(eventRepo, times(1)).findByDepartment_AccountIdAndEventTypeAndStatus(1L, EventType.MUSIC, EventStatus.PUBLIC, pageable);
    }

    @Test
    @DisplayName("Test getEventsByDepartment - Branch 2: Only EventType")
    void testGetEventsByDepartment_Branch2() {
        eventService.getEventsByDepartment(1L, EventType.MUSIC, null, pageable);
        verify(eventRepo, times(1)).findByDepartment_AccountIdAndEventType(1L, EventType.MUSIC, pageable);
    }

    @Test
    @DisplayName("Test getEventsByDepartment - Branch 3: Only Status")
    void testGetEventsByDepartment_Branch3() {
        eventService.getEventsByDepartment(1L, null, EventStatus.PUBLIC, pageable);
        verify(eventRepo, times(1)).findByDepartment_AccountIdAndStatus(1L, EventStatus.PUBLIC, pageable);
    }

    @Test
    @DisplayName("Test getEventsByDepartment - Branch 4: All filters null")
    void testGetEventsByDepartment_Branch4() {
        eventService.getEventsByDepartment(1L, (EventType) null, null, pageable);
        verify(eventRepo, times(1)).findByDepartment_AccountId(1L, pageable);
    }


    @Test
    @DisplayName("Test saveCompetitionEvent - Should set parent on schedules")
    void testSaveCompetitionEvent_SetsParent() {
        // Given
        CompetitionEvent event = new CompetitionEvent();
        EventSchedule schedule = new EventSchedule();
        event.setSchedules(List.of(schedule));
        when(eventRepo.save(event)).thenReturn(event);

        // When
        eventService.saveCompetitionEvent(event);

        // Then
        // Kiểm tra logic side-effect: event cha đã được gán cho schedule con
        assertEquals(event, schedule.getEvent());
        verify(eventRepo, times(1)).save(event);
    }

    @Test
    @DisplayName("Test updateEventStatus - Should update status when found")
    void testUpdateEventStatus_Found() {
        // Given
        when(eventRepo.findById(1L)).thenReturn(Optional.of(mockEvent));
        when(eventRepo.save(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        Event result = eventService.updateEventStatus(1L, EventStatus.PUBLIC);

        // Then
        assertNotNull(result);
        assertEquals(EventStatus.PUBLIC, result.getStatus());
        verify(eventRepo, times(1)).save(mockEvent);
    }

    @Test
    @DisplayName("Test updateEventStatus - Should throw RuntimeException when not found")
    void testUpdateEventStatus_NotFound() {
        // Given
        when(eventRepo.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            eventService.updateEventStatus(999L, EventStatus.PUBLIC);
        });
        verify(eventRepo, never()).save(any());
    }

    @Test
    @DisplayName("Test approveEvent - Should set status to PUBLIC")
    void testApproveEvent() {
        // Given
        when(eventRepo.findById(1L)).thenReturn(Optional.of(mockEvent));
        when(eventRepo.save(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        Event result = eventService.approveEvent(1L);

        // Then
        assertEquals(EventStatus.PUBLIC, result.getStatus());
        verify(eventRepo, times(1)).save(mockEvent);
    }

    @Test
    @DisplayName("Test countTotalEvents - Should return count")
    void testCountTotalEvents() {
        // Given
        when(eventRepo.count()).thenReturn(50L);

        // When
        long count = eventService.countTotalEvents();

        // Then
        assertEquals(50L, count);
    }

    // --- Tests for convertToDTO (Logic mapping phức tạp) ---

    @Test
    @DisplayName("Test convertToDTO - Organizer is Organization")
    void testConvertToDTO_OrganizerIsOrg() {
        // Given
        mockEvent.setOrganization(mockOrg);
        mockEvent.setHost(mockHost); // Vẫn set host, nhưng Org phải được ưu tiên
        when(orderService.countUniqueParticipantsByEventId(1L)).thenReturn(10);

        // When
        EventCardDTO dto = eventService.convertToDTO(mockEvent);

        // Then
        assertEquals("Test Org", dto.getOrganizer());
        assertEquals(10, dto.getRegistered());
    }


    @Test
    @DisplayName("Test convertToDTO - Organizer is Unknown (Both null)")
    void testConvertToDTO_OrganizerIsUnknown() {
        // Given
        mockEvent.setOrganization(null);
        mockEvent.setHost(null);

        // When
        EventCardDTO dto = eventService.convertToDTO(mockEvent);

        // Then
        assertEquals("Unknown", dto.getOrganizer());
    }

    @Test
    @DisplayName("Test convertToDTO - City exists")
    void testConvertToDTO_CityExists() {
        // Given
        mockEvent.setPlaces(List.of(mockPlace));

        // When
        EventCardDTO dto = eventService.convertToDTO(mockEvent);

        // Then
        assertEquals("Test Place", dto.getCity());
    }

    @Test
    @DisplayName("Test convertToDTO - City is TBA (List empty)")
    void testConvertToDTO_CityIsTBA() {
        // Given
        mockEvent.setPlaces(new ArrayList<>()); // List rỗng

        // When
        EventCardDTO dto = eventService.convertToDTO(mockEvent);

        // Then
        assertEquals("TBA", dto.getCity());
    }
    @Test
    @DisplayName("Test removeEvent - Branch 1: Should return true when exists")
    void testRemoveEvent_Branch1_Exists() {
        // Given
        when(eventRepo.existsById(1L)).thenReturn(true);
        doNothing().when(eventRepo).deleteById(1L); // Cần thiết cho verify

        // When
        boolean result = eventService.removeEvent(1L);

        // Then
        assertTrue(result);
        verify(eventRepo, times(1)).existsById(1L);
        verify(eventRepo, times(1)).deleteById(1L);
    }

    @Test
    @DisplayName("Test removeEvent - Branch 2: Should return false when not exists")
    void testRemoveEvent_Branch2_NotExists() {
        // Given
        when(eventRepo.existsById(99L)).thenReturn(false);

        // When
        boolean result = eventService.removeEvent(99L);

        // Then
        assertFalse(result);
        verify(eventRepo, times(1)).existsById(99L);
        verify(eventRepo, never()).deleteById(anyLong()); // Không được gọi xóa
    }

    @Test
    @DisplayName("Test deleteByTitle - Branch 1: Should return true when found")
    void testDeleteByTitle_Branch1_Found() {
        // Given
        when(eventRepo.findByTitle("Delete Me")).thenReturn(List.of(mockEvent));
        doNothing().when(eventRepo).deleteAll(any(List.class));

        // When
        boolean result = eventService.deleteByTitle("Delete Me");

        // Then
        assertTrue(result);
        verify(eventRepo, times(1)).findByTitle("Delete Me");
        verify(eventRepo, times(1)).deleteAll(List.of(mockEvent));
    }

    @Test
    @DisplayName("Test deleteByTitle - Branch 2: Should return false when not found")
    void testDeleteByTitle_Branch2_NotFound() {
        // Given
        when(eventRepo.findByTitle("Delete Me")).thenReturn(List.of()); // Trả về list rỗng

        // When
        boolean result = eventService.deleteByTitle("Delete Me");

        // Then
        assertFalse(result);
        verify(eventRepo, times(1)).findByTitle("Delete Me");
        verify(eventRepo, never()).deleteAll(any(List.class));
    }

    // =======================================================
    // TESTS CHO CÁC HÀM TÌM KIẾM (getFirst.../getEvent...)
    // =======================================================

    @Test
    @DisplayName("Test getFirstEventByTitle - Branch 1: Found")
    void testGetFirstEventByTitle_Branch1_Found() {
        // Given
        when(eventRepo.findByTitle("Test Event")).thenReturn(List.of(mockEvent, new Event()));

        // When
        Optional<Event> result = eventService.getFirstEventByTitle("Test Event");

        // Then
        assertTrue(result.isPresent());
        assertEquals(mockEvent, result.get()); // Đảm bảo trả về phần tử đầu tiên
    }

    @Test
    @DisplayName("Test getFirstEventByTitle - Branch 2: Not Found (Empty list)")
    void testGetFirstEventByTitle_Branch2_NotFound() {
        // Given
        when(eventRepo.findByTitle("Test Event")).thenReturn(List.of()); // Rỗng

        // When
        Optional<Event> result = eventService.getFirstEventByTitle("Test Event");

        // Then
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Test getEventByEventId - Branch 1: ID is null")
    void testGetEventByEventId_Branch1_NullId() {
        // When
        Optional<Event> result = eventService.getEventByEventId(null);

        // Then
        assertTrue(result.isEmpty());
        verify(eventRepo, never()).findById(anyLong()); // Không được gọi repo
    }

    @Test
    @DisplayName("Test getEventByEventId - Branch 2: ID is valid")
    void testGetEventByEventId_Branch2_ValidId() {
        // Given
        when(eventRepo.findById(1L)).thenReturn(Optional.of(mockEvent));

        // When
        Optional<Event> result = eventService.getEventByEventId(1L);

        // Then
        assertTrue(result.isPresent());
        verify(eventRepo, times(1)).findById(1L);
    }

    // =======================================================
    // TEST BỔ SUNG CHO convertToDTO
    // =======================================================

    @Test
    @DisplayName("Test convertToDTO - Branch: Organizer is Unknown (Host and Org are null)")
    void testConvertToDTO_Branch_OrganizerUnknown() {
        // Given
        mockEvent.setOrganization(null);
        mockEvent.setHost(null);

        // When
        EventCardDTO dto = eventService.convertToDTO(mockEvent);

        // Then
        assertEquals("Unknown", dto.getOrganizer());
    }

    // =======================================================
    // TESTS CHO HÀM searchEvents
    // =======================================================

    @Test
    @DisplayName("Test searchEvents - Branch: All parameters null (Chỉ gọi với nulls)")
    void testSearchEvents_Branch_AllNull() {
        // Given
        when(eventRepo.searchEvents(null, null, null, null)).thenReturn(List.of(mockEvent));

        // When
        List<EventCardDTO> result = eventService.searchEvents(null, null, null, null);

        // Then
        verify(eventRepo, times(1)).searchEvents(null, null, null, null);
        assertEquals(1, result.size()); // Đảm bảo convertToDTO được gọi
    }

    @Test
    @DisplayName("Test searchEvents - Branch: Blank keyword (Phải convert sang null)")
    void testSearchEvents_Branch_BlankKeyword() {
        // Given
        when(eventRepo.searchEvents(null, null, null, null)).thenReturn(List.of(mockEvent));

        // When
        // Keyword rỗng (" ") phải được coi là null
        List<EventCardDTO> result = eventService.searchEvents("   ", null, null, null);

        // Then
        verify(eventRepo, times(1)).searchEvents(null, null, null, null);
    }

    @Test
    @DisplayName("Test searchEvents - Branch: All parameters valid")
    void testSearchEvents_Branch_AllParamsValid() {
        // Given
        String keyword = "Music Fest";
        String typeStr = "MUSIC";
        LocalDate startDate = LocalDate.of(2025, 1, 1);
        LocalDate endDate = LocalDate.of(2025, 1, 31);

        // Xác định đúng LocalDateTime sẽ được truyền vào Repo
        LocalDateTime expectedStart = LocalDateTime.of(2025, 1, 1, 0, 0, 0);
        LocalDateTime expectedEnd = LocalDateTime.of(2025, 1, 31, 23, 59, 59);

        when(eventRepo.searchEvents(keyword, EventType.MUSIC, expectedStart, expectedEnd))
                .thenReturn(List.of(mockEvent));

        // When
        List<EventCardDTO> result = eventService.searchEvents(keyword, typeStr, startDate, endDate);

        // Then
        verify(eventRepo, times(1)).searchEvents(keyword, EventType.MUSIC, expectedStart, expectedEnd);
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("Test searchEvents - Branch: Invalid EventType (catch exception)")
    void testSearchEvents_Branch_InvalidType() {
        // Given
        String keyword = "Test";
        String invalidType = "INVALID_TYPE"; // Loại này không có trong Enum

        // Repo sẽ được gọi với EventType là null vì exception
        when(eventRepo.searchEvents(keyword, null, null, null)).thenReturn(List.of(mockEvent));

        // When
        List<EventCardDTO> result = eventService.searchEvents(keyword, invalidType, null, null);

        // Then
        // Quan trọng: Xác minh rằng nó vẫn gọi repo, nhưng với EventType là null
        verify(eventRepo, times(1)).searchEvents(keyword, null, null, null);
        assertEquals(1, result.size());
        // (Bạn cũng có thể kiểm tra log "⚠️ Invalid event type: INVALID_TYPE" nếu setup log)
    }
    @Test
    @DisplayName("Test getEventById - Should return Optional from repo")
    void testGetEventById() {
        when(eventRepo.findById(1L)).thenReturn(Optional.of(mockEvent));
        Optional<Event> result = eventService.getEventById(1L);
        assertTrue(result.isPresent());
        verify(eventRepo, times(1)).findById(1L);
    }

    @Test
    @DisplayName("Test getEventsByIds - Branch 1: Should return events")
    void testGetEventsByIds_Branch1_ValidList() {
        List<Long> ids = List.of(1L, 2L);
        when(eventRepo.findAllById(ids)).thenReturn(List.of(mockEvent));

        List<Event> result = eventService.getEventsByIds(ids);

        assertEquals(1, result.size());
        verify(eventRepo, times(1)).findAllById(ids);
    }

    @Test
    @DisplayName("Test getEventsByIds - Branch 2: Should return empty for null list")
    void testGetEventsByIds_Branch2_NullList() {
        List<Event> result = eventService.getEventsByIds(null);
        assertTrue(result.isEmpty());
        verify(eventRepo, never()).findAllById(any());
    }

    @Test
    @DisplayName("Test getEventsByIds - Branch 2: Should return empty for empty list")
    void testGetEventsByIds_Branch3_EmptyList() {
        List<Event> result = eventService.getEventsByIds(Collections.emptyList());
        assertTrue(result.isEmpty());
        verify(eventRepo, never()).findAllById(any());
    }

    @Test
    @DisplayName("Test getEventByHostId")
    void testGetEventByHostId() {
        when(eventRepo.getEventByHostId(1L)).thenReturn(List.of(mockEvent));
        eventService.getEventByHostId(1L);
        verify(eventRepo, times(1)).getEventByHostId(1L);
    }

    @Test
    @DisplayName("Test countUniqueParticipantsByEventId")
    void testCountUniqueParticipantsByEventId() {
        when(orderService.countUniqueParticipantsByEventId(1L)).thenReturn(25);
        long result = eventService.countUniqueParticipantsByEventId(1L);
        assertEquals(25L, result);
        verify(orderService, times(1)).countUniqueParticipantsByEventId(1L);
    }

    @Test
    @DisplayName("Test saveEvent (POJO)")
    void testSaveEvent_POJO() {
        when(eventRepo.save(mockEvent)).thenReturn(mockEvent);
        eventService.saveEvent(mockEvent);
        verify(eventRepo, times(1)).save(mockEvent);
    }

    @Test
    @DisplayName("Test getEventsByType")
    void testGetEventsByType() {
        when(eventRepo.findByEventType(MusicEvent.class)).thenReturn(List.of(mockEvent));
        eventService.getEventsByType(MusicEvent.class);
        verify(eventRepo, times(1)).findByEventType(MusicEvent.class);
    }

    @Test
    @DisplayName("Test findByTitle")
    void testFindByTitle() {
        when(eventRepo.findByTitle("Test")).thenReturn(List.of(mockEvent));
        eventService.findByTitle("Test");
        verify(eventRepo, times(1)).findByTitle("Test");
    }

    @Test
    @DisplayName("Test findByTitleAndPublicStatus")
    void testFindByTitleAndPublicStatus() {
        when(eventRepo.findByTitleAndPublicStatus("Test")).thenReturn(List.of(mockEvent));
        eventService.findByTitleAndPublicStatus("Test");
        verify(eventRepo, times(1)).findByTitleAndPublicStatus("Test");
    }

    @Test
    @DisplayName("Test getAllEvents")
    void testGetAllEvents() {
        when(eventRepo.findAll()).thenReturn(List.of(mockEvent));
        eventService.getAllEvents();
        verify(eventRepo, times(1)).findAll();
    }

    @Test
    @DisplayName("Test getFirstPublicEventByTitle - Branch 1: Found")
    void testGetFirstPublicEventByTitle_Branch1_Found() {
        when(eventRepo.findByTitleAndPublicStatus("Test")).thenReturn(List.of(mockEvent, new Event()));
        Optional<Event> result = eventService.getFirstPublicEventByTitle("Test");
        assertTrue(result.isPresent());
        assertEquals(mockEvent, result.get());
    }

    @Test
    @DisplayName("Test getFirstPublicEventByTitle - Branch 2: Not Found")
    void testGetFirstPublicEventByTitle_Branch2_NotFound() {
        when(eventRepo.findByTitleAndPublicStatus("Test")).thenReturn(List.of());
        Optional<Event> result = eventService.getFirstPublicEventByTitle("Test");
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Test getEventsByPlace")
    void testGetEventsByPlace() {
        when(eventRepo.findByPlaceId(1L)).thenReturn(List.of(mockEvent));
        eventService.getEventsByPlace(1L);
        verify(eventRepo, times(1)).findByPlaceId(1L);
    }

    @Test
    @DisplayName("Test getNextUpcomingEventByUserId")
    void testGetNextUpcomingEventByUserId() {
        when(eventRepo.findNextUpcomingEventByUserId(eq(1L), any(LocalDateTime.class))).thenReturn(Optional.of(mockEvent));
        eventService.getNextUpcomingEventByUserId(1L);
        verify(eventRepo, times(1)).findNextUpcomingEventByUserId(eq(1L), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("Test getEventByUserId")
    void testGetEventByUserId() {
        when(eventRepo.findAll()).thenReturn(List.of(mockEvent));
        // Logic hiện tại của hàm này là findAll(), nên ta test đúng như vậy
        eventService.getEventByUserId(1L);
        verify(eventRepo, times(1)).findAll();
    }

    @Test
    @DisplayName("Test countEventsByStatus")
    void testCountEventsByStatus() {
        when(eventRepo.findByStatus(EventStatus.PUBLIC)).thenReturn(List.of(mockEvent, new Event()));
        long result = eventService.countEventsByStatus(EventStatus.PUBLIC);
        assertEquals(2L, result);
    }

    @Test
    @DisplayName("Test countEventsByType")
    void testCountEventsByType() {
        // Giả lập Page trả về
        Page<Event> mockPage = new PageImpl<>(List.of(mockEvent, new Event()));
        when(eventRepo.findByEventType(eq(EventType.MUSIC), any(PageRequest.class))).thenReturn(mockPage);

        long result = eventService.countEventsByType(EventType.MUSIC);

        assertEquals(2L, result);
        verify(eventRepo, times(1)).findByEventType(eq(EventType.MUSIC), any(PageRequest.class));
    }

    @Test
    @DisplayName("Test getRecentEvents")
    void testGetRecentEvents() {
        Page<Event> mockPage = new PageImpl<>(List.of(mockEvent));
        // Phải dùng any(PageRequest.class) vì logic tạo PageRequest nằm bên trong hàm
        when(eventRepo.findAll(any(PageRequest.class))).thenReturn(mockPage);

        List<Event> result = eventService.getRecentEvents(5);

        assertEquals(1, result.size());
        verify(eventRepo, times(1)).findAll(any(PageRequest.class));
    }

    @Test
    @DisplayName("Test getPosterEvents")
    void testGetPosterEvents() {
        when(eventRepo.findByPosterTrue()).thenReturn(List.of(mockEvent));
        List<EventCardDTO> result = eventService.getPosterEvents();
        assertEquals(1, result.size());
        verify(eventRepo, times(1)).findByPosterTrue();
    }


    // =======================================================
    // TEST BỔ SUNG CHO CÁC HÀM CÒN LẠI
    // =======================================================

    @Test
    @DisplayName("Test saveMusicEvent - Should return null (as per implementation)")
    void testSaveMusicEvent() {
        // Test này chỉ đảm bảo hàm chạy đúng như code (return null)
        assertNull(eventService.saveMusicEvent(new MusicEvent()));
    }

    @Test
    @DisplayName("Test saveFestivalEvent - Should set parent on schedules")
    void testSaveFestivalEvent() {
        FestivalEvent event = new FestivalEvent();
        EventSchedule schedule = new EventSchedule();
        event.setSchedules(List.of(schedule));
        when(eventRepo.save(event)).thenReturn(event);

        eventService.saveFestivalEvent(event);
        assertEquals(event, schedule.getEvent());
        verify(eventRepo, times(1)).save(event);
    }

    @Test
    @DisplayName("Test saveWorkshopEvent - Should set parent on schedules")
    void testSaveWorkshopEvent() {
        WorkshopEvent event = new WorkshopEvent();
        EventSchedule schedule = new EventSchedule();
        event.setSchedules(List.of(schedule));
        when(eventRepo.save(event)).thenReturn(event);

        eventService.saveWorkshopEvent(event);
        assertEquals(event, schedule.getEvent());
        verify(eventRepo, times(1)).save(event);
    }

    @Test
    @DisplayName("Test isTimeConflict")
    void testIsTimeConflict() {
        LocalDateTime start = LocalDateTime.now();
        LocalDateTime end = LocalDateTime.now().plusHours(2);
        List<Place> places = List.of(mockPlace);
        when(eventRepo.findConflictedEvents(start, end, places)).thenReturn(List.of(mockEvent));

        List<Event> result = eventService.isTimeConflict(start, end, places);

        assertEquals(1, result.size());
        verify(eventRepo, times(1)).findConflictedEvents(start, end, places);
    }

    @Test
    @DisplayName("Test getEventsBetween - Logic Stream Filter")
    void testGetEventsBetween() {
        // Given
        LocalDateTime start = LocalDateTime.of(2025, 1, 10, 0, 0);
        LocalDateTime end = LocalDateTime.of(2025, 1, 20, 0, 0);

        Event eventInRange = new Event();
        eventInRange.setStartsAt(LocalDateTime.of(2025, 1, 11, 0, 0));
        eventInRange.setEndsAt(LocalDateTime.of(2025, 1, 12, 0, 0));

        Event eventOutOfRange = new Event();
        eventOutOfRange.setStartsAt(LocalDateTime.of(2025, 1, 1, 0, 0));
        eventOutOfRange.setEndsAt(LocalDateTime.of(2025, 1, 2, 0, 0));

        when(eventRepo.findAll()).thenReturn(List.of(eventInRange, eventOutOfRange));

        // When
        List<Event> result = eventService.getEventsBetween(start, end, 1L);

        // Then
        assertEquals(1, result.size());
        assertEquals(eventInRange, result.get(0));
    }

    @Test
    @DisplayName("Test convertToDTO - Branch: Organizer is Host's Organization")
    void testConvertToDTO_Branch_OrganizerIsHostOrg() {
        // Given
        // Nhánh 2 (bị thiếu): Event.Org = null, NHƯNG Host.Org != null
        mockHost.setOrganization(mockOrg); // Host thuộc Org
        mockEvent.setOrganization(null);  // Event không có Org trực tiếp
        mockEvent.setHost(mockHost);      // Event có Host

        // When
        EventCardDTO dto = eventService.convertToDTO(mockEvent);

        // Then
        // Phải lấy tên Org từ Host
        assertEquals("Test Org", dto.getOrganizer());
    }








}
