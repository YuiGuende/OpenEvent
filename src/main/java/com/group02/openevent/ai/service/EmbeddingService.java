package com.group02.openevent.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;

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

    private boolean isEnabled = true;

    // 1. Dùng Constructor để Inject các giá trị cấu hình từ application.properties/dotenv
    public EmbeddingService(@Value("${api.token:}") String apiToken) {
        // Gán các giá trị được inject (đã được dotenv-java phân giải)
        this.apiToken = apiToken;
        // Các giá trị cố định có thể đặt trực tiếp ở đây
        this.apiUrl = "https://router.huggingface.co/nebius/v1/embeddings";
        this.modelId = "Qwen/Qwen3-Embedding-8B";
        
        // Kiểm tra API token
        if (apiToken == null || apiToken.trim().isEmpty()) {
            this.isEnabled = false;
            System.out.println("⚠️ WARNING: HUGGINGFACE_TOKEN not configured. Embedding service will be disabled.");
        }
    }

    public float[] getEmbedding(String input) throws Exception {
        if (!isEnabled) {
            throw new IllegalStateException("Embedding service is disabled. Please configure HUGGINGFACE_TOKEN in environment variables.");
        }
        if (input == null || input.isBlank()) {
            throw new IllegalArgumentException("Input text cannot be null or empty.");
        }
        // Gọi lại hàm xử lý theo lô với danh sách chỉ có 1 phần tử
        List<float[]> embeddings = getEmbeddings(List.of(input));
        if (embeddings.isEmpty()) {
            throw new RuntimeException("Failed to get embedding for single text.");
        }
        return embeddings.get(0);
    }

    public List<float[]> getEmbeddings(List<String> texts) throws Exception {
        if (!isEnabled) {
            throw new IllegalStateException("Embedding service is disabled. Please configure HUGGINGFACE_TOKEN in environment variables.");
        }
        if (texts == null || texts.isEmpty()) {
            return Collections.emptyList();
        }

        // 1. Chuẩn bị body với danh sách các input
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", modelId);
        requestBody.put("input", texts); // Gửi cả danh sách `texts`

        String jsonRequest = mapper.writeValueAsString(requestBody);

        // 2. Tạo và gửi yêu cầu API
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .header("Authorization", "Bearer " + apiToken)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonRequest))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 401 || response.statusCode() == 403) {
            String errorMsg = "Embedding API authentication failed. Please check your HUGGINGFACE_TOKEN. Status: " + response.statusCode();
            if (response.body() != null && !response.body().isEmpty()) {
                errorMsg += " Response: " + response.body();
            }
            throw new IllegalStateException(errorMsg);
        }
        
        if (response.statusCode() != 200) {
            throw new RuntimeException("Embedding API request failed with status " + response.statusCode() + ": " + response.body());
        }

        // 3. Xử lý kết quả trả về là một danh sách các vector
        JsonNode root = mapper.readTree(response.body());
        JsonNode dataArray = root.get("data");

        List<float[]> results = new ArrayList<>();
        if (dataArray.isArray()) {
            for (JsonNode dataNode : dataArray) {
                JsonNode embeddingArray = dataNode.get("embedding");
                if (embeddingArray.isArray()) {
                    float[] vector = new float[embeddingArray.size()];
                    for (int i = 0; i < embeddingArray.size(); i++) {
                        vector[i] = embeddingArray.get(i).floatValue();
                    }
                    results.add(vector);
                }
            }
        }
        return results;
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