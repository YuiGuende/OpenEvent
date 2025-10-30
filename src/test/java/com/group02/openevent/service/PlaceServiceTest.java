package com.group02.openevent.service;

import com.group02.openevent.model.enums.Building;
import com.group02.openevent.model.event.Place;
import com.group02.openevent.repository.IPlaceRepo;
import com.group02.openevent.service.impl.PlaceServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class PlaceServiceTest {
    @Mock
    IPlaceRepo placeRepo;

    @InjectMocks
    PlaceServiceImpl placeService;

    @Test
    void TC01_ShouldReturnPlaces_WhenJPQLHasResults() {
        // GIVEN
        Long eventId = 1L;
        Place place = new Place();
        place.setId(1L);
        place.setPlaceName("A");
        place.setBuilding(Building.ALPHA);

        List<Place> jpqlPlaces = List.of(place);

        when(placeRepo.findAll()).thenReturn(List.of(new Place()));
        when(placeRepo.findPlacesByEventId(eventId)).thenReturn(jpqlPlaces);

        // WHEN
        List<Place> result = placeService.getAllByEventId(eventId);

        // THEN
        assertEquals(1, result.size());
        assertEquals("A", result.get(0).getPlaceName());
        verify(placeRepo).findPlacesByEventId(eventId);
        verify(placeRepo, never()).findPlacesByEventIdNative(any());
    }


    @Test
    void TC02_ShouldFallbackToNativeQuery_WhenJPQLReturnsEmpty() {
        // GIVEN
        Long eventId = 2L;

        Place place = new Place();
        place.setId(2L);
        place.setPlaceName("Hall B");
        place.setBuilding(Building.GAMMA);

        when(placeRepo.findAll()).thenReturn(List.of(new Place()));
        when(placeRepo.findPlacesByEventId(eventId)).thenReturn(Collections.emptyList());
        when(placeRepo.findPlacesByEventIdNative(eventId)).thenReturn(List.of(place));

        // WHEN
        List<Place> result = placeService.getAllByEventId(eventId);

        // THEN
        assertEquals(1, result.size());
        assertEquals("Hall B", result.get(0).getPlaceName());
        verify(placeRepo).findPlacesByEventId(eventId);
        verify(placeRepo).findPlacesByEventIdNative(eventId);
    }


    @Test
    void TC03_ShouldReturnEmpty_WhenBothQueriesEmpty() {
        // GIVEN
        Long eventId = 3L;
        when(placeRepo.findAll()).thenReturn(List.of(new Place()));
        when(placeRepo.findPlacesByEventId(eventId)).thenReturn(Collections.emptyList());
        when(placeRepo.findPlacesByEventIdNative(eventId)).thenReturn(Collections.emptyList());

        // WHEN
        List<Place> result = placeService.getAllByEventId(eventId);

        // THEN
        assertTrue(result.isEmpty());
        verify(placeRepo).findPlacesByEventId(eventId);
        verify(placeRepo).findPlacesByEventIdNative(eventId);
    }

    @Test
    void TC04_ShouldStillReturnEmpty_WhenDBHasPlacesButNoEventMatch() {
        // GIVEN
        Long eventId = 99L;
        when(placeRepo.findAll()).thenReturn(List.of(new Place(1L, "A", "B1")));
        when(placeRepo.findPlacesByEventId(eventId)).thenReturn(Collections.emptyList());
        when(placeRepo.findPlacesByEventIdNative(eventId)).thenReturn(Collections.emptyList());

        // WHEN
        List<Place> result = placeService.getAllByEventId(eventId);

        // THEN
        assertTrue(result.isEmpty());
        verify(placeRepo).findAll();
        verify(placeRepo).findPlacesByEventId(eventId);
        verify(placeRepo).findPlacesByEventIdNative(eventId);
    }

    @Test
    void TC05_ShouldCallJPQLThenNativeInOrder_WhenJPQLEmpty() {
        // GIVEN
        Long eventId = 5L;
        when(placeRepo.findAll()).thenReturn(List.of());
        when(placeRepo.findPlacesByEventId(eventId)).thenReturn(Collections.emptyList());
        when(placeRepo.findPlacesByEventIdNative(eventId))
                .thenReturn(List.of(new Place(5L, "Fallback", "F1")));

        // WHEN
        placeService.getAllByEventId(eventId);

        // THEN
        InOrder inOrder = inOrder(placeRepo);
        inOrder.verify(placeRepo).findAll();
        inOrder.verify(placeRepo).findPlacesByEventId(eventId);
        inOrder.verify(placeRepo).findPlacesByEventIdNative(eventId);
    }
}
