package com.group02.openevent.service.impl;

import com.group02.openevent.dto.request.update.EventUpdateRequest;
import com.group02.openevent.dto.request.PlaceUpdateRequest;
import com.group02.openevent.model.enums.Building;
import com.group02.openevent.dto.response.EventResponse;
import com.group02.openevent.model.event.*;
import com.group02.openevent.model.enums.EventStatus;
import com.group02.openevent.model.enums.EventType;
import com.group02.openevent.model.organization.Organization;
import com.group02.openevent.model.user.Host;
import com.group02.openevent.repository.*;
import com.group02.openevent.mapper.EventMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Test cases cho Event Update Feature
 * Feature: Event update feature (Lê Huỳnh Đức)
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Event Update Feature - Service Layer Tests")
class EventServiceImplUpdateTest {

    @Mock
    private IEventRepo eventRepo;
    @Mock
    private IOrganizationRepo organizationRepo;
    @Mock
    private IHostRepo hostRepo;
    @Mock
    private IPlaceRepo placeRepo;
    @Mock
    private EventMapper eventMapper;

    @InjectMocks
    private EventServiceImpl eventService;

    private Event existingEvent;
    private EventUpdateRequest updateRequest;
    private static final Long EVENT_ID = 1L;
    private static final Long HOST_ID = 2L;
    private static final Long ORGANIZATION_ID = 3L;

    @BeforeEach
    void setUp() {
        existingEvent = new Event();
        existingEvent.setId(EVENT_ID);
        existingEvent.setTitle("Original Event");
        existingEvent.setDescription("Original Description");
        existingEvent.setStatus(EventStatus.DRAFT);
        existingEvent.setEventType(EventType.WORKSHOP);
        existingEvent.setStartsAt(LocalDateTime.now().plusDays(1));
        existingEvent.setEndsAt(LocalDateTime.now().plusDays(2));
        existingEvent.setCapacity(100);

        updateRequest = EventUpdateRequest.builder()
                .id(EVENT_ID)
                .title("Updated Event")
                .description("Updated Description")
                .eventType(EventType.WORKSHOP)
                .build();
    }

    @Nested
    @DisplayName("TC-EU-01 to TC-EU-05: Basic Event Update Tests")
    class BasicUpdateTests {

        @Test
        @DisplayName("TC-EU-01: Update event successfully with basic fields")
        void updateEvent_BasicFields_Success() {
            // Arrange
            when(eventRepo.findById(EVENT_ID)).thenReturn(Optional.of(existingEvent));
            when(eventRepo.saveAndFlush(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));
            doNothing().when(eventMapper).updateEventFromRequest(any(), any());

            EventResponse mockResponse = EventResponse.builder()
                    .id(EVENT_ID.intValue())
                    .title("Updated Event")
                    .build();
            when(eventMapper.toEventResponse(any(Event.class))).thenReturn(mockResponse);

            // Act
            EventResponse result = eventService.updateEvent(EVENT_ID, updateRequest);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(EVENT_ID);
            verify(eventRepo).findById(EVENT_ID);
            verify(eventMapper).updateEventFromRequest(updateRequest, existingEvent);
            verify(eventRepo).saveAndFlush(any(Event.class));
        }

        @Test
        @DisplayName("TC-EU-02: Update event fails when event not found")
        void updateEvent_EventNotFound_ThrowsException() {
            // Arrange
            when(eventRepo.findById(EVENT_ID)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> eventService.updateEvent(EVENT_ID, updateRequest))
                    .isInstanceOf(jakarta.persistence.EntityNotFoundException.class)
                    .hasMessageContaining("Event not found");
            verify(eventRepo).findById(EVENT_ID);
            verify(eventRepo, never()).saveAndFlush(any());
        }

        @Test
        @DisplayName("TC-EU-03: Update event with organization successfully")
        void updateEvent_WithOrganization_Success() {
            // Arrange
            Organization org = new Organization();
            org.setOrgId(ORGANIZATION_ID);
            updateRequest.setOrganizationId(ORGANIZATION_ID);

            when(eventRepo.findById(EVENT_ID)).thenReturn(Optional.of(existingEvent));
            when(organizationRepo.findById(ORGANIZATION_ID)).thenReturn(Optional.of(org));
            when(eventRepo.saveAndFlush(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));
            doNothing().when(eventMapper).updateEventFromRequest(any(), any());

            EventResponse mockResponse = EventResponse.builder()
                    .id(EVENT_ID.intValue())
                    .build();
            when(eventMapper.toEventResponse(any(Event.class))).thenReturn(mockResponse);

            // Act
            EventResponse result = eventService.updateEvent(EVENT_ID, updateRequest);

            // Assert
            assertThat(result).isNotNull();
            verify(organizationRepo).findById(ORGANIZATION_ID);
            verify(eventRepo).saveAndFlush(any(Event.class));
        }

        @Test
        @DisplayName("TC-EU-04: Update event with organization fails when organization not found")
        void updateEvent_OrganizationNotFound_ThrowsException() {
            // Arrange
            updateRequest.setOrganizationId(ORGANIZATION_ID);
            when(eventRepo.findById(EVENT_ID)).thenReturn(Optional.of(existingEvent));
            when(organizationRepo.findById(ORGANIZATION_ID)).thenReturn(Optional.empty());
            doNothing().when(eventMapper).updateEventFromRequest(any(), any());

            // Act & Assert
            assertThatThrownBy(() -> eventService.updateEvent(EVENT_ID, updateRequest))
                    .isInstanceOf(jakarta.persistence.EntityNotFoundException.class)
                    .hasMessageContaining("Organization not found");
            verify(organizationRepo).findById(ORGANIZATION_ID);
            verify(eventRepo, never()).saveAndFlush(any());
        }

        @Test
        @DisplayName("TC-EU-05: Update event with host successfully")
        void updateEvent_WithHost_Success() {
            // Arrange
            Host host = new Host();
            host.setId(HOST_ID);
            updateRequest.setHostId(HOST_ID);

            when(eventRepo.findById(EVENT_ID)).thenReturn(Optional.of(existingEvent));
            when(hostRepo.findById(HOST_ID)).thenReturn(Optional.of(host));
            when(eventRepo.saveAndFlush(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));
            doNothing().when(eventMapper).updateEventFromRequest(any(), any());

            EventResponse mockResponse = EventResponse.builder()
                    .id(EVENT_ID.intValue())
                    .build();
            when(eventMapper.toEventResponse(any(Event.class))).thenReturn(mockResponse);

            // Act
            EventResponse result = eventService.updateEvent(EVENT_ID, updateRequest);

            // Assert
            assertThat(result).isNotNull();
            verify(hostRepo).findById(HOST_ID);
            verify(eventRepo).saveAndFlush(any(Event.class));
        }
    }

    @Nested
    @DisplayName("TC-EU-06 to TC-EU-10: Event Type Specific Update Tests")
    class EventTypeSpecificTests {

        @Test
        @DisplayName("TC-EU-06: Update Workshop event with specific fields")
        void updateEvent_WorkshopEvent_Success() {
            // Arrange
            WorkshopEvent workshopEvent = new WorkshopEvent();
            workshopEvent.setId(EVENT_ID);
            workshopEvent.setTitle("Workshop Event");
            workshopEvent.setEventType(EventType.WORKSHOP);

            updateRequest.setEventType(EventType.WORKSHOP);
            updateRequest.setTopic("Java Programming");
            updateRequest.setSkillLevel("Beginner");
            updateRequest.setMaxParticipants(50);

            when(eventRepo.findById(EVENT_ID)).thenReturn(Optional.of(workshopEvent));
            when(eventRepo.saveAndFlush(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));
            doNothing().when(eventMapper).updateEventFromRequest(any(), any());

            EventResponse mockResponse = EventResponse.builder()
                    .id(EVENT_ID.intValue())
                    .eventType(EventType.WORKSHOP)
                    .build();
            when(eventMapper.toEventResponse(any(Event.class))).thenReturn(mockResponse);

            // Act
            EventResponse result = eventService.updateEvent(EVENT_ID, updateRequest);

            // Assert
            assertThat(result).isNotNull();
            assertThat(workshopEvent.getTopic()).isEqualTo("Java Programming");
            assertThat(workshopEvent.getSkillLevel()).isEqualTo("Beginner");
            assertThat(workshopEvent.getMaxParticipants()).isEqualTo(50);
            verify(eventRepo).saveAndFlush(any(Event.class));
        }

        @Test
        @DisplayName("TC-EU-07: Update Competition event with specific fields")
        void updateEvent_CompetitionEvent_Success() {
            // Arrange
            CompetitionEvent competitionEvent = new CompetitionEvent();
            competitionEvent.setId(EVENT_ID);
            competitionEvent.setEventType(EventType.COMPETITION);

            updateRequest.setEventType(EventType.COMPETITION);
            updateRequest.setCompetitionType("Coding Contest");
            updateRequest.setRules("No cheating allowed");
            updateRequest.setPrizePool("$1000");

            when(eventRepo.findById(EVENT_ID)).thenReturn(Optional.of(competitionEvent));
            when(eventRepo.saveAndFlush(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));
            doNothing().when(eventMapper).updateEventFromRequest(any(), any());

            EventResponse mockResponse = EventResponse.builder()
                    .id(EVENT_ID.intValue())
                    .eventType(EventType.COMPETITION)
                    .build();
            when(eventMapper.toEventResponse(any(Event.class))).thenReturn(mockResponse);

            // Act
            EventResponse result = eventService.updateEvent(EVENT_ID, updateRequest);

            // Assert
            assertThat(result).isNotNull();
            assertThat(competitionEvent.getCompetitionType()).isEqualTo("Coding Contest");
            assertThat(competitionEvent.getRules()).isEqualTo("No cheating allowed");
            assertThat(competitionEvent.getPrizePool()).isEqualTo("$1000");
            verify(eventRepo).saveAndFlush(any(Event.class));
        }

        @Test
        @DisplayName("TC-EU-08: Update Music event with specific fields")
        void updateEvent_MusicEvent_Success() {
            // Arrange
            MusicEvent musicEvent = new MusicEvent();
            musicEvent.setId(EVENT_ID);
            musicEvent.setEventType(EventType.MUSIC);

            updateRequest.setEventType(EventType.MUSIC);
            updateRequest.setMusicType("Concert");
            updateRequest.setGenre("Rock");
            updateRequest.setPerformerCount(5);

            when(eventRepo.findById(EVENT_ID)).thenReturn(Optional.of(musicEvent));
            when(eventRepo.saveAndFlush(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));
            doNothing().when(eventMapper).updateEventFromRequest(any(), any());

            EventResponse mockResponse = EventResponse.builder()
                    .id(EVENT_ID.intValue())
                    .eventType(EventType.MUSIC)
                    .build();
            when(eventMapper.toEventResponse(any(Event.class))).thenReturn(mockResponse);

            // Act
            EventResponse result = eventService.updateEvent(EVENT_ID, updateRequest);

            // Assert
            assertThat(result).isNotNull();
            assertThat(musicEvent.getMusicType()).isEqualTo("Concert");
            assertThat(musicEvent.getGenre()).isEqualTo("Rock");
            assertThat(musicEvent.getPerformerCount()).isEqualTo(5);
            verify(eventRepo).saveAndFlush(any(Event.class));
        }

        @Test
        @DisplayName("TC-EU-09: Update Festival event with specific fields")
        void updateEvent_FestivalEvent_Success() {
            // Arrange
            FestivalEvent festivalEvent = new FestivalEvent();
            festivalEvent.setId(EVENT_ID);
            festivalEvent.setEventType(EventType.FESTIVAL);

            updateRequest.setEventType(EventType.FESTIVAL);
            updateRequest.setCulture("Vietnamese");
            updateRequest.setHighlight("Traditional Music");

            when(eventRepo.findById(EVENT_ID)).thenReturn(Optional.of(festivalEvent));
            when(eventRepo.saveAndFlush(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));
            doNothing().when(eventMapper).updateEventFromRequest(any(), any());

            EventResponse mockResponse = EventResponse.builder()
                    .id(EVENT_ID.intValue())
                    .eventType(EventType.FESTIVAL)
                    .build();
            when(eventMapper.toEventResponse(any(Event.class))).thenReturn(mockResponse);

            // Act
            EventResponse result = eventService.updateEvent(EVENT_ID, updateRequest);

            // Assert
            assertThat(result).isNotNull();
            assertThat(festivalEvent.getCulture()).isEqualTo("Vietnamese");
            assertThat(festivalEvent.getHighlight()).isEqualTo("Traditional Music");
            verify(eventRepo).saveAndFlush(any(Event.class));
        }

        @Test
        @DisplayName("TC-EU-10: Update event with parent event successfully")
        void updateEvent_WithParentEvent_Success() {
            // Arrange
            Event parentEvent = new Event();
            parentEvent.setId(10L);
            updateRequest.setParentEventId(10L);

            when(eventRepo.findById(EVENT_ID)).thenReturn(Optional.of(existingEvent));
            when(eventRepo.findById(10L)).thenReturn(Optional.of(parentEvent));
            when(eventRepo.saveAndFlush(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));
            doNothing().when(eventMapper).updateEventFromRequest(any(), any());

            EventResponse mockResponse = EventResponse.builder()
                    .id(EVENT_ID.intValue())
                    .build();
            when(eventMapper.toEventResponse(any(Event.class))).thenReturn(mockResponse);

            // Act
            EventResponse result = eventService.updateEvent(EVENT_ID, updateRequest);

            // Assert
            assertThat(result).isNotNull();
            verify(eventRepo).findById(10L);
            verify(eventRepo).saveAndFlush(any(Event.class));
        }
    }

    @Nested
    @DisplayName("TC-EU-11 to TC-EU-15: Places Update Tests")
    class PlacesUpdateTests {

        @Test
        @DisplayName("TC-EU-11: Update event with new places successfully")
        void updateEvent_WithNewPlaces_Success() {
            // Arrange
            List<PlaceUpdateRequest> placeRequests = new ArrayList<>();
            PlaceUpdateRequest place1 = new PlaceUpdateRequest();
            place1.setPlaceName("Room A");
            place1.setBuilding(Building.ALPHA);
            place1.setIsDeleted(false);
            placeRequests.add(place1);

            updateRequest.setPlaceUpdateRequests(placeRequests);
            existingEvent.setPlaces(new ArrayList<>());

            Place savedPlace = new Place();
            savedPlace.setId(1L);
            savedPlace.setPlaceName("Room A");
            savedPlace.setBuilding(Building.ALPHA);

            when(eventRepo.findById(EVENT_ID)).thenReturn(Optional.of(existingEvent));
            when(placeRepo.save(any(Place.class))).thenReturn(savedPlace);
            when(eventRepo.saveAndFlush(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));
            doNothing().when(eventMapper).updateEventFromRequest(any(), any());

            EventResponse mockResponse = EventResponse.builder()
                    .id(EVENT_ID.intValue())
                    .build();
            when(eventMapper.toEventResponse(any(Event.class))).thenReturn(mockResponse);

            // Act
            EventResponse result = eventService.updateEvent(EVENT_ID, updateRequest);

            // Assert
            assertThat(result).isNotNull();
            verify(placeRepo).save(any(Place.class));
            verify(eventRepo).saveAndFlush(any(Event.class));
        }

        @Test
        @DisplayName("TC-EU-12: Update event with existing places successfully")
        void updateEvent_WithExistingPlaces_Success() {
            // Arrange
            Place existingPlace = new Place();
            existingPlace.setId(1L);
            existingPlace.setPlaceName("Old Room");
            existingPlace.setBuilding(Building.BETA);

            List<PlaceUpdateRequest> placeRequests = new ArrayList<>();
            PlaceUpdateRequest placeUpdate = new PlaceUpdateRequest();
            placeUpdate.setId(1L);
            placeUpdate.setPlaceName("Updated Room");
            placeUpdate.setBuilding(Building.GAMMA);
            placeUpdate.setIsDeleted(false);
            placeRequests.add(placeUpdate);

            updateRequest.setPlaceUpdateRequests(placeRequests);
            existingEvent.setPlaces(new ArrayList<>());

            when(eventRepo.findById(EVENT_ID)).thenReturn(Optional.of(existingEvent));
            when(placeRepo.findById(1L)).thenReturn(Optional.of(existingPlace));
            when(placeRepo.save(any(Place.class))).thenReturn(existingPlace);
            when(eventRepo.saveAndFlush(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));
            doNothing().when(eventMapper).updateEventFromRequest(any(), any());

            EventResponse mockResponse = EventResponse.builder()
                    .id(EVENT_ID.intValue())
                    .build();
            when(eventMapper.toEventResponse(any(Event.class))).thenReturn(mockResponse);

            // Act
            EventResponse result = eventService.updateEvent(EVENT_ID, updateRequest);

            // Assert
            assertThat(result).isNotNull();
            assertThat(existingPlace.getPlaceName()).isEqualTo("Updated Room");
            assertThat(existingPlace.getBuilding()).isEqualTo(Building.GAMMA);
            verify(placeRepo).findById(1L);
            verify(placeRepo).save(existingPlace);
        }

        @Test
        @DisplayName("TC-EU-13: Update event removes deleted places successfully")
        void updateEvent_RemoveDeletedPlaces_Success() {
            // Arrange
            List<PlaceUpdateRequest> placeRequests = new ArrayList<>();
            PlaceUpdateRequest placeToDelete = new PlaceUpdateRequest();
            placeToDelete.setId(1L);
            placeToDelete.setIsDeleted(true);
            placeRequests.add(placeToDelete);

            updateRequest.setPlaceUpdateRequests(placeRequests);
            existingEvent.setPlaces(new ArrayList<>());

            when(eventRepo.findById(EVENT_ID)).thenReturn(Optional.of(existingEvent));
            when(eventRepo.saveAndFlush(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));
            doNothing().when(eventMapper).updateEventFromRequest(any(), any());

            EventResponse mockResponse = EventResponse.builder()
                    .id(EVENT_ID.intValue())
                    .build();
            when(eventMapper.toEventResponse(any(Event.class))).thenReturn(mockResponse);

            // Act
            EventResponse result = eventService.updateEvent(EVENT_ID, updateRequest);

            // Assert
            assertThat(result).isNotNull();
            verify(placeRepo, never()).findById(1L);
            verify(eventRepo).saveAndFlush(any(Event.class));
        }

        @Test
        @DisplayName("TC-EU-14: Update event with place not found throws exception")
        void updateEvent_PlaceNotFound_ThrowsException() {
            // Arrange
            List<PlaceUpdateRequest> placeRequests = new ArrayList<>();
            PlaceUpdateRequest placeUpdate = new PlaceUpdateRequest();
            placeUpdate.setId(999L);
            placeUpdate.setPlaceName("Non-existent Room");
            placeUpdate.setBuilding(Building.ALPHA);
            placeUpdate.setIsDeleted(false);
            placeRequests.add(placeUpdate);

            updateRequest.setPlaceUpdateRequests(placeRequests);
            existingEvent.setPlaces(new ArrayList<>());

            when(eventRepo.findById(EVENT_ID)).thenReturn(Optional.of(existingEvent));
            when(placeRepo.findById(999L)).thenReturn(Optional.empty());
            doNothing().when(eventMapper).updateEventFromRequest(any(), any());

            // Act & Assert
            assertThatThrownBy(() -> eventService.updateEvent(EVENT_ID, updateRequest))
                    .isInstanceOf(jakarta.persistence.EntityNotFoundException.class)
                    .hasMessageContaining("Place not found");
            verify(placeRepo).findById(999L);
            verify(eventRepo, never()).saveAndFlush(any());
        }

        @Test
        @DisplayName("TC-EU-15: Update event with empty places list clears all places")
        void updateEvent_EmptyPlacesList_ClearsPlaces() {
            // Arrange
            Place existingPlace = new Place();
            existingPlace.setId(1L);
            existingEvent.setPlaces(new ArrayList<>(List.of(existingPlace)));

            updateRequest.setPlaceUpdateRequests(new ArrayList<>());

            when(eventRepo.findById(EVENT_ID)).thenReturn(Optional.of(existingEvent));
            when(eventRepo.saveAndFlush(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));
            doNothing().when(eventMapper).updateEventFromRequest(any(), any());

            EventResponse mockResponse = EventResponse.builder()
                    .id(EVENT_ID.intValue())
                    .build();
            when(eventMapper.toEventResponse(any(Event.class))).thenReturn(mockResponse);

            // Act
            EventResponse result = eventService.updateEvent(EVENT_ID, updateRequest);

            // Assert
            assertThat(result).isNotNull();
            verify(eventRepo).saveAndFlush(any(Event.class));
        }
    }
}

