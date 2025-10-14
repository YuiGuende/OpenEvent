package com.group02.openevent.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.group02.openevent.ai.util.ConfigLoader;
import org.springframework.beans.factory.annotation.Value;
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


    // Khai báo các biến không phải là static final
    private final String apiToken;
    private final String apiUrl;
    private final String modelId;

    // Static fields có thể giữ nguyên nếu không phụ thuộc vào cấu hình
    private static final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    // 1. Dùng Constructor để Inject các giá trị cấu hình từ application.properties/dotenv
    public EmbeddingService(@Value("${api.token}") String apiToken) {
        // Gán các giá trị được inject (đã được dotenv-java phân giải)
        this.apiToken = apiToken;
        // Các giá trị cố định có thể đặt trực tiếp ở đây
        this.apiUrl = "https://router.huggingface.co/nebius/v1/embeddings";
        this.modelId = "Qwen/Qwen3-Embedding-8B";
    }

    public float[] getEmbedding(String input) throws Exception {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", modelId);
        requestBody.put("input", List.of(input)); // dạng array

        String jsonRequest = mapper.writeValueAsString(requestBody);


        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .header("Authorization", "Bearer " + apiToken)
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