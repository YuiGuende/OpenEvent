package com.group02.openevent.ai.controller;

import com.group02.openevent.ai.qdrant.service.QdrantService;
import com.group02.openevent.ai.qdrant.service.VectorIntentClassifier;
import com.group02.openevent.ai.qdrant.model.ActionType;
import com.group02.openevent.ai.service.EmbeddingService;
import com.group02.openevent.ai.service.WeatherService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

class AIUtilityControllerTest {

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
    void embedding_ok_and_blank_400() throws Exception {
        when(embeddingService.getEmbedding(anyString())).thenReturn(new float[]{1f,2f});
        mockMvc.perform(post("/api/ai/utility/embedding")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"text\":\"hi\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.embeddingSize").value(2));

        mockMvc.perform(post("/api/ai/utility/embedding")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"text\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void similarity_ok_and_missing_400() throws Exception {
        when(embeddingService.getEmbedding(anyString())).thenReturn(new float[]{1f,0f});
        when(embeddingService.cosineSimilarity(any(), any())).thenReturn(0.5);
        mockMvc.perform(post("/api/ai/utility/similarity")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"text1\":\"a\",\"text2\":\"b\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.similarity").value(0.5));

        mockMvc.perform(post("/api/ai/utility/similarity")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"text1\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void classify_intent_ok_and_blank_400() throws Exception {
        when(embeddingService.getEmbedding(anyString())).thenReturn(new float[]{1f});
        when(intentClassifier.classifyIntent(anyString(), any())).thenReturn(ActionType.PROMPT_FREE_TIME);
        when(intentClassifier.classifyWeather(anyString(), any())).thenReturn("RAINY");
        when(intentClassifier.classifyToolEvent(anyString(), any())).thenReturn("ADD_EVENT");

        mockMvc.perform(post("/api/ai/utility/classify-intent")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"text\":\"schedule\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.actionType").value("PROMPT_FREE_TIME"));

        mockMvc.perform(post("/api/ai/utility/classify-intent")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"text\":\" \"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void vector_search_ok_and_blank_400() throws Exception {
        when(embeddingService.getEmbedding(anyString())).thenReturn(new float[]{1f,2f,3f});
        when(qdrantService.searchSimilarVectors(any(), anyInt())).thenReturn(List.of(Map.of("id", 1)));
        mockMvc.perform(post("/api/ai/utility/vector-search")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"query\":\"hello\",\"topK\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCount").value(1));

        mockMvc.perform(post("/api/ai/utility/vector-search")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"query\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void upload_vector_ok_and_missing_400() throws Exception {
        when(embeddingService.getEmbedding(anyString())).thenReturn(new float[]{1f});
        when(qdrantService.upsertEmbedding(anyString(), any(), any())).thenReturn("ok");
        mockMvc.perform(post("/api/ai/utility/upload-vector")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"id\":\"1\",\"text\":\"t\",\"payload\":{}}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("ok"));

        mockMvc.perform(post("/api/ai/utility/upload-vector")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"id\":\"1\",\"text\":\"t\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void health_ok() throws Exception {
        when(embeddingService.getEmbedding(anyString())).thenReturn(new float[]{1f,2f});
        when(weatherService.getForecastNote(any(), anyString())).thenReturn("sunny");
        when(intentClassifier.classifyIntent(anyString(), any())).thenReturn(ActionType.PROMPT_FREE_TIME);
        when(qdrantService.searchSimilarVectors(any(), anyInt())).thenReturn(List.of());
        mockMvc.perform(get("/api/ai/utility/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.overall").exists());
    }
}


