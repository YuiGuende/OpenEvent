package com.group02.openevent.ai.qdrant.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.Mockito.*;

class VectorSeedRunnerTest {
    @Mock EventVectorSyncService syncService;
    @Mock QdrantService qdrantService;
    @InjectMocks VectorSeedRunner runner;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
        // Set field directly (no Spring)
        try {
            var field = VectorSeedRunner.class.getDeclaredField("runFullSeed");
            field.setAccessible(true);
            field.set(runner, false);
        } catch (Exception ignored) {}
    }

    @Test
    void run_noop_ifFlagFalse() throws Exception {
        runner.run(); // nothing to assert, just trigger
    }

    @Test
    void run_fullSeed_callsAll() throws Exception {
        var field = VectorSeedRunner.class.getDeclaredField("runFullSeed");
        field.setAccessible(true);
        field.set(runner, true);
        doNothing().when(qdrantService).createPayloadIndex(anyString(), anyString());
        doNothing().when(syncService).syncAllEvents();
        doNothing().when(syncService).seedAllPlaces();
        doNothing().when(syncService).seedPromptSummary();
        doNothing().when(syncService).seedPromptSendEmail();
        doNothing().when(syncService).seedOutdoorActivities();
        doNothing().when(syncService).seedPromptAddEvent();
        runner.run();
        verify(qdrantService).createPayloadIndex(eq("kind"), anyString());
        verify(syncService).syncAllEvents();
        verify(syncService).seedAllPlaces();
        verify(syncService).seedPromptSummary();
        verify(syncService).seedPromptSendEmail();
        verify(syncService).seedOutdoorActivities();
        verify(syncService).seedPromptAddEvent();
    }
}





