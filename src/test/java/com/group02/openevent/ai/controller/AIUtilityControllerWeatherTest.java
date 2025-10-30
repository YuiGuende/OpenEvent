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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AIUtilityControllerWeatherTest {

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
    void weather_ok_and_malformed_date_500() throws Exception {
        when(weatherService.getForecastNote(any(), anyString())).thenReturn("ok");
        mockMvc.perform(post("/api/ai/utility/weather")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"location\":\"Da Nang\",\"date\":\"2025-01-01T10:00:00\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/ai/utility/weather")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"location\":\"Da Nang\",\"date\":\"invalid\"}"))
                .andExpect(status().isInternalServerError());
    }
}


