package com.group02.openevent.service;

import com.group02.openevent.model.enums.Building;
import com.group02.openevent.model.event.Place;
import com.group02.openevent.repository.IPlaceRepo;
import com.group02.openevent.service.impl.PlaceServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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
import java.util.Optional;

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

    private Place mockPlace;

    @BeforeEach
    void setUp() {
        // Dữ liệu mock chung
        mockPlace = new Place();
        mockPlace.setId(1L);
        mockPlace.setPlaceName("Tòa nhà FPT");
        mockPlace.setBuilding(Building.ALPHA);
    }
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
    @Test
    @DisplayName("Test findPlaceById - Found")
    void testFindPlaceById_Found() {
        // Given
        when(placeRepo.findById(1L)).thenReturn(Optional.of(mockPlace));

        // When
        Optional<Place> result = placeService.findPlaceById(1L);

        // Then
        assertTrue(result.isPresent());
        assertEquals("Tòa nhà FPT", result.get().getPlaceName());
        verify(placeRepo, times(1)).findById(1L);
    }

    @Test
    @DisplayName("Test findPlaceById - Not Found")
    void testFindPlaceById_NotFound() {
        // Given
        when(placeRepo.findById(99L)).thenReturn(Optional.empty());

        // When
        Optional<Place> result = placeService.findPlaceById(99L);

        // Then
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Test findPlaceByName - Found")
    void testFindPlaceByName_Found() {
        // Given
        when(placeRepo.findByPlaceName("Tòa nhà FPT")).thenReturn(Optional.of(mockPlace));

        // When
        Optional<Place> result = placeService.findPlaceByName("Tòa nhà FPT");

        // Then
        assertTrue(result.isPresent());
        verify(placeRepo, times(1)).findByPlaceName("Tòa nhà FPT");
    }

    @Test
    @DisplayName("Test findAllPlaces - Success")
    void testFindAllPlaces() {
        // Given
        when(placeRepo.findAll()).thenReturn(List.of(mockPlace, new Place()));

        // When
        List<Place> result = placeService.findAllPlaces();

        // Then
        assertEquals(2, result.size());
        verify(placeRepo, times(1)).findAll();
    }

    // --- Tests for findPlaceByNameFlexible (0% coverage, complex logic) ---

    @Test
    @DisplayName("Test findPlaceByNameFlexible - Branch 1: Found by Cleaned Name")
    void testFindPlaceByNameFlexible_Branch1_FoundByCleanedName() {
        // Given: Input là "Tòa FPT", cleaned là "FPT"
        when(placeRepo.findByPlaceNameContainingIgnoreCase("FPT")).thenReturn(List.of(mockPlace));

        // When
        Optional<Place> result = placeService.findPlaceByNameFlexible("Tòa FPT");

        // Then
        assertTrue(result.isPresent());
        assertEquals("Tòa nhà FPT", result.get().getPlaceName());

        // Xác minh chỉ tìm kiếm "cleaned" được gọi
        verify(placeRepo, times(1)).findByPlaceNameContainingIgnoreCase("FPT");
        verify(placeRepo, never()).findByPlaceNameContainingIgnoreCase("Tòa FPT");
        verify(placeRepo, never()).findByPlaceName(anyString());
    }

    @Test
    @DisplayName("Test findPlaceByNameFlexible - Branch 2: Found by Original Name")
    void testFindPlaceByNameFlexible_Branch2_FoundByOriginalName() {
        // Given: Input là "Tòa FPT", cleaned là "FPT"
        when(placeRepo.findByPlaceNameContainingIgnoreCase("FPT")).thenReturn(List.of()); // Cleaned search fails
        when(placeRepo.findByPlaceNameContainingIgnoreCase("Tòa FPT")).thenReturn(List.of(mockPlace)); // Original search succeeds

        // When
        Optional<Place> result = placeService.findPlaceByNameFlexible("Tòa FPT");

        // Then
        assertTrue(result.isPresent());

        // Xác minh cả hai tìm kiếm "containing" đều được gọi
        verify(placeRepo, times(1)).findByPlaceNameContainingIgnoreCase("FPT");
        verify(placeRepo, times(1)).findByPlaceNameContainingIgnoreCase("Tòa FPT");
        verify(placeRepo, never()).findByPlaceName(anyString());
    }

    @Test
    @DisplayName("Test findPlaceByNameFlexible - Branch 3: Found by Part Name")
    void testFindPlaceByNameFlexible_Branch3_FoundByPartName() {
        // Given: Input "Beta Hall"
        when(placeRepo.findByPlaceNameContainingIgnoreCase("Beta Hall")).thenReturn(List.of()); // Cleaned/Original fails
        when(placeRepo.findByPlaceNameContainingIgnoreCase("Beta")).thenReturn(List.of(mockPlace)); // Part search succeeds

        // When
        Optional<Place> result = placeService.findPlaceByNameFlexible("Beta Hall");

        // Then
        assertTrue(result.isPresent());

        // Xác minh 3 lần gọi (Original, Part1)
        verify(placeRepo, times(1)).findByPlaceNameContainingIgnoreCase("Beta Hall");
        verify(placeRepo, times(1)).findByPlaceNameContainingIgnoreCase("Beta");
        verify(placeRepo, never()).findByPlaceName(anyString());
    }

    @Test
    @DisplayName("Test findPlaceByNameFlexible - Branch 4: Found by Exact Name")
    void testFindPlaceByNameFlexible_Branch4_FoundByExactName() {
        // Given: Input "Delta"
        when(placeRepo.findByPlaceNameContainingIgnoreCase("Delta")).thenReturn(List.of()); // All "containing" searches fail
        when(placeRepo.findByPlaceName("Delta")).thenReturn(Optional.of(mockPlace)); // Exact search succeeds

        // When
        Optional<Place> result = placeService.findPlaceByNameFlexible("Delta");

        // Then
        assertTrue(result.isPresent());

        verify(placeRepo, times(2)).findByPlaceNameContainingIgnoreCase("Delta");
        verify(placeRepo, times(1)).findByPlaceName("Delta");
    }

    @Test
    @DisplayName("Test findPlaceByNameFlexible - Branch 5: Not Found")
    void testFindPlaceByNameFlexible_Branch5_NotFound() {
        // Given: Input "Omega"
        when(placeRepo.findByPlaceNameContainingIgnoreCase("Omega")).thenReturn(List.of()); // Containing fails
        when(placeRepo.findByPlaceName("Omega")).thenReturn(Optional.empty()); // Exact fails

        // When
        Optional<Place> result = placeService.findPlaceByNameFlexible("Omega");

        // Then
        assertTrue(result.isEmpty());

        // Xác minh 2 lần gọi (Containing và Exact)
        verify(placeRepo, times(2)).findByPlaceNameContainingIgnoreCase("Omega");
        verify(placeRepo, times(1)).findByPlaceName("Omega");
    }

}
