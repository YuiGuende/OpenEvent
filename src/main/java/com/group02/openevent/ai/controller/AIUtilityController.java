package com.group02.openevent.ai.controller;

import com.group02.openevent.ai.service.EmbeddingService;
import com.group02.openevent.ai.service.WeatherService;
import com.group02.openevent.ai.qdrant.service.QdrantService;
import com.group02.openevent.ai.qdrant.service.VectorIntentClassifier;
import com.group02.openevent.ai.qdrant.model.ActionType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller để xử lý các utility functions của AI
 * @author Admin
 */
@RestController
@RequestMapping("/api/ai/utility")
@CrossOrigin(origins = "*")
public class AIUtilityController {

    private final EmbeddingService embeddingService;
    private final WeatherService weatherService;
    private final QdrantService qdrantService;
    private final VectorIntentClassifier intentClassifier;

    public AIUtilityController(EmbeddingService embeddingService,
                               WeatherService weatherService,
                               QdrantService qdrantService,
                               VectorIntentClassifier intentClassifier) {
        this.embeddingService = embeddingService;
        this.weatherService = weatherService;
        this.qdrantService = qdrantService;
        this.intentClassifier = intentClassifier;
    }

    /**
     * Tạo embedding cho text
     *
     * @param request Map chứa text
     * @return ResponseEntity chứa embedding vector
     */
    @PostMapping("/embedding")
    public ResponseEntity<Map<String, Object>> generateEmbedding(@RequestBody Map<String, String> request) {
        try {
            String text = request.get("text");

            if (text == null || text.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "❌ Text không được để trống"));
            }

            float[] embedding = embeddingService.getEmbedding(text);

            Map<String, Object> result = Map.of(
                    "success", true,
                    "text", text,
                    "embeddingSize", embedding.length,
                    "embedding", embedding
            );

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> result = Map.of(
                    "success", false,
                    "error", "❌ Lỗi khi tạo embedding: " + e.getMessage()
            );
            return ResponseEntity.internalServerError().body(result);
        }
    }

    /**
     * Tính cosine similarity giữa hai text
     *
     * @param request Map chứa hai text
     * @return ResponseEntity chứa similarity score
     */
    @PostMapping("/similarity")
    public ResponseEntity<Map<String, Object>> calculateSimilarity(@RequestBody Map<String, String> request) {
        try {
            String text1 = request.get("text1");
            String text2 = request.get("text2");

            if (text1 == null || text2 == null || text1.trim().isEmpty() || text2.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "❌ Cả hai text không được để trống"));
            }

            float[] embedding1 = embeddingService.getEmbedding(text1);
            float[] embedding2 = embeddingService.getEmbedding(text2);

            double similarity = embeddingService.cosineSimilarity(embedding1, embedding2);

            Map<String, Object> result = Map.of(
                    "success", true,
                    "text1", text1,
                    "text2", text2,
                    "similarity", similarity,
                    "similarityPercentage", String.format("%.2f%%", similarity * 100)
            );

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> result = Map.of(
                    "success", false,
                    "error", "❌ Lỗi khi tính similarity: " + e.getMessage()
            );
            return ResponseEntity.internalServerError().body(result);
        }
    }

    /**
     * Phân loại intent của text
     *
     * @param request Map chứa text
     * @return ResponseEntity chứa intent classification
     */
    @PostMapping("/classify-intent")
    public ResponseEntity<Map<String, Object>> classifyIntent(@RequestBody Map<String, String> request) {
        try {
            String text = request.get("text");

            if (text == null || text.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "❌ Text không được để trống"));
            }
            float[] textVector = embeddingService.getEmbedding(text);
            ActionType intent = intentClassifier.classifyIntent(text, textVector);
            String weatherIntent = intentClassifier.classifyWeather(text, textVector);
            String toolIntent = intentClassifier.classifyToolEvent(text,  textVector);

            Map<String, Object> result = Map.of(
                    "success", true,
                    "text", text,
                    "actionType", intent.toString(),
                    "weatherIntent", weatherIntent,
                    "toolIntent", toolIntent
            );

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> result = Map.of(
                    "success", false,
                    "error", "❌ Lỗi khi phân loại intent: " + e.getMessage()
            );
            return ResponseEntity.internalServerError().body(result);
        }
    }

    /**
     * Lấy thông tin thời tiết
     *
     * @param request Map chứa thông tin địa điểm và thời gian
     * @return ResponseEntity chứa thông tin thời tiết
     */
    @PostMapping("/weather")
    public ResponseEntity<Map<String, Object>> getWeather(@RequestBody Map<String, Object> request) {
        try {
            String location = (String) request.getOrDefault("location", "Da Nang");
            String dateStr = (String) request.getOrDefault("date", LocalDateTime.now().toString());

            LocalDateTime date = LocalDateTime.parse(dateStr);
            String forecast = weatherService.getForecastNote(date, location);

            Map<String, Object> result = Map.of(
                    "success", true,
                    "location", location,
                    "date", dateStr,
                    "forecast", forecast != null ? forecast : "Không có dự báo thời tiết",
                    "hasWeatherWarning", forecast != null && !forecast.isEmpty()
            );

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> result = Map.of(
                    "success", false,
                    "error", "❌ Lỗi khi lấy thông tin thời tiết: " + e.getMessage()
            );
            return ResponseEntity.internalServerError().body(result);
        }
    }

    /**
     * Tìm kiếm vector tương tự trong Qdrant
     *
     * @param request Map chứa query text
     * @return ResponseEntity chứa kết quả tìm kiếm
     */
    @PostMapping("/vector-search")
    public ResponseEntity<Map<String, Object>> vectorSearch(@RequestBody Map<String, Object> request) {
        try {
            String queryText = (String) request.get("query");
            Integer topK = (Integer) request.getOrDefault("topK", 5);

            if (queryText == null || queryText.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "❌ Query text không được để trống"));
            }

            float[] queryVector = embeddingService.getEmbedding(queryText);
            List<Map<String, Object>> results = qdrantService.searchSimilarVectors(queryVector, topK);

            Map<String, Object> result = Map.of(
                    "success", true,
                    "query", queryText,
                    "topK", topK,
                    "results", results,
                    "resultCount", results.size()
            );

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> result = Map.of(
                    "success", false,
                    "error", "❌ Lỗi khi tìm kiếm vector: " + e.getMessage()
            );
            return ResponseEntity.internalServerError().body(result);
        }
    }

    /**
     * Upload vector vào Qdrant
     *
     * @param request Map chứa thông tin vector
     * @return ResponseEntity chứa kết quả upload
     */
    @PostMapping("/upload-vector")
    public ResponseEntity<Map<String, Object>> uploadVector(@RequestBody Map<String, Object> request) {
        try {
            String id = (String) request.get("id");
            String text = (String) request.get("text");
            @SuppressWarnings("unchecked")
            Map<String, Object> payload = (Map<String, Object>) request.get("payload");

            if (id == null || text == null || payload == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "❌ Thiếu thông tin id, text hoặc payload"));
            }

            float[] embedding = embeddingService.getEmbedding(text);
            String result = qdrantService.upsertEmbedding(id, embedding, payload);

            Map<String, Object> response = Map.of(
                    "success", true,
                    "id", id,
                    "text", text,
                    "result", result
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> result = Map.of(
                    "success", false,
                    "error", "❌ Lỗi khi upload vector: " + e.getMessage()
            );
            return ResponseEntity.internalServerError().body(result);
        }
    }

    /**
     * Kiểm tra health của các AI services
     *
     * @return ResponseEntity chứa trạng thái health
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> checkHealth() {
        Map<String, Object> health = new HashMap<>();
        float[] testVector = null;
        try {
            // Kiểm tra EmbeddingService
            try {
                // Lấy vector và lưu lại để dùng cho Qdrant
                testVector = embeddingService.getEmbedding("test health check");
                int vectorSize = testVector.length;
                health.put("embeddingService", Map.of("status", "UP", "vectorSize", vectorSize));
            } catch (Exception e) {
                health.put("embeddingService", Map.of("status", "DOWN", "error", e.getMessage()));
            }

            // Kiểm tra WeatherService
            try {
                String testWeather = weatherService.getForecastNote(LocalDateTime.now(), "Da Nang");
                health.put("weatherService", Map.of("status", "UP", "testResult", testWeather != null));
            } catch (Exception e) {
                health.put("weatherService", Map.of("status", "DOWN", "error", e.getMessage()));
            }

            // Kiểm tra VectorIntentClassifier
            try {
                ActionType testIntent = intentClassifier.classifyIntent("test", testVector);
                health.put("intentClassifier", Map.of("status", "UP", "testIntent", testIntent.toString()));
            } catch (Exception e) {
                health.put("intentClassifier", Map.of("status", "DOWN", "error", e.getMessage()));
            }

            if (testVector != null && testVector.length > 1) {
                try {
                    // Sử dụng testVector đã tạo thành công ở bước 1
                    List<Map<String, Object>> testSearch = qdrantService.searchSimilarVectors(testVector, 1);
                    health.put("qdrantService", Map.of("status", "UP", "searchResult", testSearch.size()));
                } catch (Exception e) {
                    health.put("qdrantService", Map.of("status", "DOWN", "error", "Qdrant error: " + e.getMessage()));
                }
            } else {
                // Chỉ định rõ nếu Qdrant không thể test do Embedding lỗi
                if ("DOWN".equals(((Map<String, Object>)health.get("embeddingService")).get("status"))) {
                    health.put("qdrantService", Map.of("status", "DOWN", "error", "Cannot test Qdrant: EmbeddingService is DOWN."));
                } else {
                    // Trường hợp này rất hiếm, nhưng để đảm bảo logic
                    health.put("qdrantService", Map.of("status", "DOWN", "error", "Cannot test Qdrant: testVector has size <= 1."));
                }
            }

            boolean allUp = health.values().stream()
                    .filter(v -> v instanceof Map) // Chỉ xử lý các giá trị là Map
                    .allMatch(v -> "UP".equals(((Map<?, ?>) v).get("status")));

            health.put("timestamp", LocalDateTime.now().toString());
            health.put("overall", allUp ? "UP" : "DOWN");

            return ResponseEntity.ok(health);

        } catch (Exception e) {
            // Lỗi xảy ra bên ngoài các block try/catch con (ít xảy ra)
            e.printStackTrace();
            Map<String, Object> errorHealth = Map.of(
                    "overall", "DOWN",
                    "error", "Internal error during health check: " + e.getMessage(),
                    "timestamp", LocalDateTime.now().toString()
            );
            return ResponseEntity.internalServerError().body(errorHealth);
        }
    }
}
