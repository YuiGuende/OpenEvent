package com.group02.openevent.ai.controller;

import com.group02.openevent.ai.qdrant.service.QdrantService;
import com.group02.openevent.ai.qdrant.service.VectorIntentClassifier;
import com.group02.openevent.ai.service.EmbeddingService;
import com.group02.openevent.ai.service.WeatherService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AIUtilityControllerHealthDownTest {

    @Mock private EmbeddingService embeddingService;
    @Mock private WeatherService weatherService;
    @Mock private QdrantService qdrantService;
    @Mock private VectorIntentClassifier intentClassifier;

    @InjectMocks private AIUtilityController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
        mockMvc = org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void health_down_when_embedding_throws() throws Exception {
        when(embeddingService.getEmbedding(anyString())).thenThrow(new RuntimeException("down"));
        mockMvc.perform(get("/api/ai/utility/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.overall").value("DOWN"));
    }
}


