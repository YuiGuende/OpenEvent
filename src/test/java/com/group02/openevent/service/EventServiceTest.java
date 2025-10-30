package com.group02.openevent.service;

import com.group02.openevent.dto.request.PlaceUpdateRequest;
import com.group02.openevent.dto.request.create.EventCreationRequest;
import com.group02.openevent.dto.request.update.EventUpdateRequest;
import com.group02.openevent.dto.response.EventResponse;
import com.group02.openevent.mapper.EventMapper;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
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








}
