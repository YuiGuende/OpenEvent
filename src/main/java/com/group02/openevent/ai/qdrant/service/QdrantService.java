package com.group02.openevent.ai.qdrant.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.group02.openevent.ai.util.ConfigLoader;
import lombok.extern.slf4j.Slf4j;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.net.URI;
import java.net.http.*;
import java.util.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.qdrant.client.grpc.Points.ScoredPoint;

/**
 * Service để tương tác với Qdrant vector database
 * @author Admin
 */
@Service
@Slf4j
public class QdrantService {
    private final String baseUrl;
    private final String apiKey;
    private final String collection;
    private final int vectorSize; // Giữ lại như final

    private final HttpClient http = HttpClient.newHttpClient();
    private final ObjectMapper om = new ObjectMapper();

    // Constructor TIÊM (INJECT) các giá trị cấu hình từ Spring
    public QdrantService(
            @Value("${qdrant.url}") String baseUrl,
            @Value("${qdrant.api.key}") String apiKey,
            @Value("${qdrant.collection}") String collection,
            @Value("${qdrant.vector.size}") int vectorSize // Đảm bảo bạn có qdrant.vector.size trong config
    ) {
        this.baseUrl = baseUrl.trim();
        this.apiKey = apiKey.trim();
        this.collection = collection.trim();
        this.vectorSize = vectorSize;
    }


    @PostConstruct
    public void initializeIndexes() {
        log.info("Bắt đầu khởi tạo các chỉ mục (index) cần thiết cho Qdrant collection '{}'...", collection);
        try {
            // Đảm bảo collection tồn tại trước khi tạo index
            ensureCollection();

            // Tạo index cho các trường cần lọc để tăng tốc độ truy vấn
            createPayloadIndex("startsAt", "integer");
            createPayloadIndex("kind", "keyword");
            createPayloadIndex("place_id", "integer"); // Thêm index cho place_id nếu cần

            log.info("✅ Hoàn tất khởi tạo chỉ mục cho Qdrant.");
        } catch (Exception e) {
            // Lỗi này thường xảy ra nếu index đã tồn tại, có thể bỏ qua một cách an toàn
            log.warn("⚠️ Không thể tạo chỉ mục Qdrant. Có thể chỉ mục đã tồn tại hoặc có lỗi kết nối. Lỗi: {}", e.getMessage());
        }
    }

    // Gọi trước khi upsert/search
    public void ensureCollection() throws Exception {
        HttpRequest get = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/collections/" + collection))
                .header("api-key", apiKey)
                .build();
        HttpResponse<String> resp = http.send(get, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() == 200) return; // đã có

        // tạo mới
        var body = Map.of(
                "vectors", Map.of("size", vectorSize, "distance", "Cosine")
        );
        HttpRequest put = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/collections/" + collection))
                .header("Content-Type", "application/json")
                .header("api-key", apiKey)
                .PUT(HttpRequest.BodyPublishers.ofString(om.writeValueAsString(body)))
                .build();
        HttpResponse<String> created = http.send(put, HttpResponse.BodyHandlers.ofString());
        requireOk(created, "Tạo collection thất bại");
    }

    public String upsertEmbedding(String id, float[] embedding, Map<String, Object> payload) throws Exception {
        List<Float> vec = new ArrayList<>(embedding.length);
        for (float v : embedding) vec.add(v);

        Object qId;
        try { qId = Long.valueOf(id); }
        catch (NumberFormatException e) { qId = id; }

        Map<String, Object> point = new HashMap<>();
        point.put("id", qId);
        point.put("vector", vec);
        if (payload != null) point.put("payload", payload);

        // Gọi hàm upsert theo lô với danh sách chỉ có 1 point
        upsertPoints(List.of(point));

        return "{\"status\":\"ok\"}";
    }

    /**
     * Xóa một embedding (vector) khỏi Qdrant bằng ID của nó.
     * @param pointId ID của điểm (vector) cần xóa (chính là eventId đã chuyển thành String)
     */
    public void deleteEmbedding(String pointId) throws Exception {
        if (pointId == null || pointId.isBlank()) {
            log.warn("deleteEmbedding được gọi với ID rỗng, bỏ qua.");
            return;
        }

        log.info("Attempting to delete point ID {} from collection '{}'", pointId, collection);
        ensureCollection();

        // 1. Chuyển đổi ID sang định dạng Qdrant (giống hệt logic upsert)
        Object qId;
        try {
            qId = Long.valueOf(pointId);
        } catch (NumberFormatException e) {
            qId = pointId; // Giữ nguyên là String nếu không phải là số
        }

        // 2. Tạo body cho request, Qdrant yêu cầu một danh sách các ID
        Map<String, Object> reqBody = Map.of(
                "points", List.of(qId)
        );

        // 3. Tạo HTTP POST request
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/collections/" + collection + "/points/delete?wait=true")) // wait=true để đảm bảo xóa xong
                .header("Content-Type", "application/json")
                .header("api-key", apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(om.writeValueAsString(reqBody)))
                .build();

        // 4. Gửi request và kiểm tra lỗi
        HttpResponse<String> resp = http.send(request, HttpResponse.BodyHandlers.ofString());
        requireOk(resp, "Lỗi khi xóa point ID: " + pointId);
        log.info("✅ Xóa thành công point ID {} khỏi collection '{}'", pointId, collection);
    }

    public List<Map<String,Object>> searchSimilarVectors(float[] queryVector, int limit) throws Exception {
        ensureCollection();

        List<Float> vec = new ArrayList<>(queryVector.length);
        for (float v : queryVector) vec.add(v);

        Map<String,Object> req = new HashMap<>();
        req.put("vector", vec);
        req.put("limit", limit);
        req.put("with_payload", true);

        HttpRequest httpReq = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/collections/" + collection + "/points/search"))
                .header("Content-Type", "application/json")
                .header("api-key", apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(om.writeValueAsString(req)))
                .build();

        HttpResponse<String> resp = http.send(httpReq, HttpResponse.BodyHandlers.ofString());
        requireOk(resp, "Search lỗi");

        JsonNode root = om.readTree(resp.body());
        List<Map<String,Object>> out = new ArrayList<>();
        for (JsonNode n : root.path("result")) {
            Map<String,Object> m = new HashMap<>();
            m.put("id", n.path("id").isNumber() ? n.get("id").asLong() : n.get("id").asText());
            m.put("score", n.path("score").asDouble());
            if (n.has("payload")) m.put("payload", om.convertValue(n.get("payload"), Map.class));
            out.add(m);
        }
        return out;
    }
    /**
     * Tìm kiếm Vector với bộ lọc Metadata (Filtering).
     * Đây là hàm tổng quát được sử dụng bởi VectorIntentClassifier và EventVectorSearchService.
     * @param queryVector Vector tìm kiếm.
     * @param limit Số lượng kết quả.
     * @param filter Bộ lọc metadata Qdrant (dưới dạng Map<String, Object>).
     * @return Danh sách các điểm (point) Qdrant phù hợp.
     */
    public List<Map<String, Object>> searchSimilarVectorsWithFilter(
            float[] queryVector,
            int limit,
            Map<String, Object> filter) throws Exception {

        ensureCollection();

        List<Float> vec = new ArrayList<>(queryVector.length);
        for (float v : queryVector) vec.add(v);

        Map<String, Object> req = new HashMap<>();
        req.put("vector", vec);
        req.put("limit", limit);
        req.put("with_payload", true);

        // ÁP DỤNG BỘ LỌC TÙY CHỈNH
        if (filter != null && !filter.isEmpty()) {
            req.put("filter", filter);
        }

        HttpRequest httpReq = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/collections/" + collection + "/points/search"))
                .header("Content-Type", "application/json")
                .header("api-key", apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(om.writeValueAsString(req)))
                .build();

        HttpResponse<String> resp = http.send(httpReq, HttpResponse.BodyHandlers.ofString());
        requireOk(resp, "Search lỗi (có filter)");

        // Tái sử dụng logic parse kết quả JSON
        JsonNode root = om.readTree(resp.body());
        List<Map<String,Object>> out = new ArrayList<>();
        for (JsonNode n : root.path("result")) {
            Map<String,Object> m = new HashMap<>();
            m.put("id", n.path("id").isNumber() ? n.get("id").asLong() : n.get("id").asText());
            m.put("score", n.path("score").asDouble());
            if (n.has("payload")) m.put("payload", om.convertValue(n.get("payload"), Map.class));
            out.add(m);
        }
        return out;
    }

    private void requireOk(HttpResponse<String> resp, String msg) {
        if (resp.statusCode() != 200) throw new RuntimeException(msg + ": HTTP " + resp.statusCode() + " - " + resp.body());
        try {
            JsonNode root = om.readTree(resp.body());
            if (root.has("status") && !root.path("status").path("error").isNull()
                    && !root.path("status").path("error").asText().isEmpty()) {
                throw new RuntimeException(msg + ": " + root.path("status").path("error").asText());
            }
        } catch (Exception ignore) { /* nếu body không phải JSON hợp lệ, đã check statusCode ở trên */ }
    }
    /**
     * Tìm kiếm các địa điểm Place gần nhất bằng Vector Search trong Qdrant.
     */
    public List<Map<String, Object>> searchPlacesByVector(float[] queryVector, int limit) throws Exception {
        // 1. Chuẩn bị Vector
        List<Float> vec = new ArrayList<>(queryVector.length);
        for (float v : queryVector) vec.add(v);

        Map<String, Object> req = new HashMap<>();
        req.put("vector", vec);
        req.put("limit", limit);
        req.put("with_payload", true);

        // THÊM FILTER: Nếu bạn có payload "kind:place" trong Qdrant
        // req.put("filter", Map.of("must", List.of(Map.of("key", "kind", "match", Map.of("value", "place")))));
        Map<String, Object> filter = Map.of(
                "must", List.of(
                        // Yêu cầu trường 'kind' trong payload phải có giá trị là 'place'
                        Map.of("key", "kind", "match", Map.of("value", "place"))
                )
        );
        req.put("filter", filter); // <-- Thêm bộ lọc vào request

        // 2. Gọi API Qdrant (Sử dụng logic từ searchSimilarVectors)
        HttpRequest httpReq = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/collections/" + collection + "/points/search"))
                .header("Content-Type", "application/json")
                .header("api-key", apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(om.writeValueAsString(req)))
                .build();

        HttpResponse<String> resp = http.send(httpReq, HttpResponse.BodyHandlers.ofString());

        // Tái sử dụng logic kiểm tra lỗi và parsing JSON từ searchSimilarVectors
        requireOk(resp, "Search Places lỗi");

        // Logic parse JSON (Giống hệt searchSimilarVectors)
        JsonNode root = om.readTree(resp.body());
        List<Map<String,Object>> out = new ArrayList<>();
        for (JsonNode n : root.path("result")) {
            Map<String,Object> m = new HashMap<>();
            m.put("id", n.path("id").isNumber() ? n.get("id").asLong() : n.get("id").asText());
            m.put("score", n.path("score").asDouble());
            if (n.has("payload")) m.put("payload", om.convertValue(n.get("payload"), Map.class));
            out.add(m);
        }
        return out;
    }
    /**
     * Tạo Payload Index cho một key cụ thể (ví dụ: 'kind') để tối ưu hóa việc lọc.
     */
    public void createPayloadIndex(String fieldName, String fieldType) throws Exception {
        String indexUrl = baseUrl + "/collections/" + collection + "/index";

        var body = Map.of(
                "field_name", fieldName,
                "field_schema", fieldType, // Ví dụ: "keyword" hoặc "integer"
                "wait", true
        );

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(indexUrl))
                .header("Content-Type", "application/json")
                .header("api-key", apiKey)
                .PUT(HttpRequest.BodyPublishers.ofString(om.writeValueAsString(body)))
                .build();

        HttpResponse<String> resp = http.send(request, HttpResponse.BodyHandlers.ofString());
        requireOk(resp, "Tạo Payload Index thất bại cho key: " + fieldName);
    }
    /**
     * ✅ PHƯƠNG THỨC MỚI QUAN TRỌNG:
     * Upsert một danh sách các điểm (points) lên Qdrant trong một lần gọi API duy nhất.
     */
    public void upsertPoints(List<Map<String, Object>> points) throws Exception {
        if (points == null || points.isEmpty()) {
            log.warn("upsertPoints được gọi với danh sách rỗng, bỏ qua.");
            return;
        }

        ensureCollection();

        // Body của request sẽ chứa một danh sách các point trong key "points"
        Map<String, Object> req = Map.of("points", points);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/collections/" + collection + "/points?wait=true")) // wait=true để đảm bảo dữ liệu được ghi xong
                .header("Content-Type", "application/json")
                .header("api-key", apiKey)
                .PUT(HttpRequest.BodyPublishers.ofString(om.writeValueAsString(req)))
                .build();

        HttpResponse<String> resp = http.send(request, HttpResponse.BodyHandlers.ofString());
        requireOk(resp, "Lỗi khi upsert theo lô (batch upsert)");
        log.info("✅ Upsert thành công {} points vào collection '{}'", points.size(), collection);
    }

}
