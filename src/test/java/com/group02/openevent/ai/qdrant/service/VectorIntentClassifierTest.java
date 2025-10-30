package com.group02.openevent.ai.qdrant.service;

import com.group02.openevent.ai.service.EmbeddingService;
import com.group02.openevent.ai.service.EventVectorSearchService;
import com.group02.openevent.model.event.Event;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.*;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class VectorIntentClassifierTest {
    @Mock QdrantService qdrantService;
    @Mock EmbeddingService embeddingService;
    @Mock EventVectorSearchService eventVectorSearchService;
    @InjectMocks VectorIntentClassifier classifier;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @ParameterizedTest(name = "extractEventName found={0}")
    @MethodSource("eventNameCases")
    void extractEventName_various(boolean hasResult, String expect) throws Exception {
        String userInput = "concert";
        if (hasResult) {
            Event ev = new Event(); ev.setTitle(expect);
            when(eventVectorSearchService.searchEvents(any(), anyLong(), anyInt())).thenReturn(List.of(ev));
        } else {
            when(eventVectorSearchService.searchEvents(any(), anyLong(), anyInt())).thenReturn(Collections.emptyList());
        }
        String res = classifier.extractEventName(userInput);
        assertThat(res).isEqualTo(expect);
    }
    static Stream<Arguments> eventNameCases() {
        return Stream.of(
            Arguments.of(true, "GALA Show"),
            Arguments.of(false, "")
        );
    }
}




