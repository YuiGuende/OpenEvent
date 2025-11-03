package com.group02.openevent.ai.qdrant.service;

import com.group02.openevent.ai.service.EmbeddingService;
import com.group02.openevent.model.event.Event;
import com.group02.openevent.service.EventService;
import com.group02.openevent.service.PlaceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;

import static org.mockito.Mockito.*;

class EventVectorSyncServiceTest {
    @Mock QdrantService qdrantService;
    @Mock EventService eventService;
    @Mock EmbeddingService embeddingService;
    @Mock PlaceService placeService;
    @InjectMocks EventVectorSyncService syncService;

    @BeforeEach void setup() { MockitoAnnotations.openMocks(this); }

    @Test
    void syncAllEvents_happy() throws Exception {
        when(eventService.getAllEvents()).thenReturn(List.of(new Event()));
        when(qdrantService.upsertEmbedding(anyString(), any(float[].class), anyMap())).thenReturn("ok");
        // Will not throw
        syncService.syncAllEvents();
    }

    @Test
    void syncAllEvents_serviceThrows() {
        when(eventService.getAllEvents()).thenThrow(new RuntimeException("error"));
        try { syncService.syncAllEvents(); } catch (Exception ignored) {}
    }
}




