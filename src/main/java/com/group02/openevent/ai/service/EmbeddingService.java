package com.group02.openevent.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.group02.openevent.ai.util.ConfigLoader;
import org.springframework.stereotype.Service;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service để tạo embeddings từ text sử dụng HuggingFace API
 * @author Admin
 */
@Service
public class EmbeddingService {


    private static final String API_TOKEN =ConfigLoader.get("API_TOKEN");
    private static final String API_URL ="https://router.huggingface.co/nebius/v1/embeddings";
    private static final String MODEL_ID = "Qwen/Qwen3-Embedding-8B";
    private static final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public float[] getEmbedding(String input) throws Exception {
        System.out.println(API_TOKEN);
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", MODEL_ID);
        requestBody.put("input", List.of(input)); // dạng array

        String jsonRequest = mapper.writeValueAsString(requestBody);


        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("Authorization", "Bearer " + API_TOKEN)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonRequest))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());


        if (response.statusCode() != 200) {
            throw new RuntimeException("Failed to get embedding: " + response.body());
        }

        // Parse kết quả JSON
        JsonNode jsonNode = mapper.readTree(response.body());
        JsonNode embeddingArray = jsonNode.get("data").get(0).get("embedding");

        float[] vector = new float[embeddingArray.size()];
        for (int i = 0; i < embeddingArray.size(); i++) {
            vector[i] = embeddingArray.get(i).floatValue();
        }

        return vector;
    }

    public double cosineSimilarity(float[] a, float[] b) {
        if (a.length != b.length) {
            throw new IllegalArgumentException("Vectors must be same length");
        }
        double dot = 0.0, normA = 0.0, normB = 0.0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }
}

