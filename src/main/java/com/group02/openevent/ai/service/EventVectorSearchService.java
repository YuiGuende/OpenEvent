package com.group02.openevent.ai.service;

import com.group02.openevent.ai.qdrant.service.QdrantService;
import com.group02.openevent.model.event.Event;
import com.group02.openevent.service.EventService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.Objects;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EventVectorSearchService {

    private final QdrantService qdrantService;
    private final EmbeddingService embeddingService;
    private final EventService eventService;

    /**
     * Tìm kiếm sự kiện theo mô tả (Vector Search) và chỉ lấy các sự kiện sắp diễn ra.
     * @param description Mô tả tìm kiếm (tiêu đề sự kiện).
     * @param userId ID người dùng (dùng cho lọc sau này).
     * @param limit Giới hạn kết quả.
     * @return Danh sách các Event Entity khớp nhất.
     */
    public List<Event> searchEvents(String description, int userId, int limit) throws Exception {

        // 1. CHUẨN BỊ BỘ LỌC THỜI GIAN (Lọc sự kiện trong quá khứ)
        // Yêu cầu sự kiện PHẢI BẮT ĐẦU SAU thời điểm hiện tại.
        // Chuyển LocalDateTime.now() thành Unix Timestamp (giây)
        long currentTimestamp = Instant.now().getEpochSecond();

        Map<String, Object> timeFilter = Map.of(
                "key", "startsAt", // Tên trường trong Payload của Qdrant
                "range", Map.of("gt", currentTimestamp) // gt = greater than (Lớn hơn thời điểm hiện tại)
        );

        // 2. CHUẨN BỊ BỘ LỌC CHÍNH (Filter cho Qdrant)
        Map<String, Object> filter = Map.of(
                "must", List.of(
                        // Chỉ tìm kiếm các vector sự kiện
                        Map.of("key", "kind", "match", Map.of("value", "event")),
                        timeFilter
                )
                // Có thể thêm lọc theo userId nếu sự kiện có payload "user_id"
        );

        // 3. TẠO VECTOR TRUY VẤN
        float[] queryVector = embeddingService.getEmbedding(description);

        // 4. GỌI QDRANT VỚI FILTER
        List<Map<String, Object>> qdrantResults =
                qdrantService.searchSimilarVectorsWithFilter(queryVector, limit, filter);

        // 5. CHUYỂN KẾT QUẢ QDRANT THÀNH ENTITY
        // Lấy Event ID từ payload Qdrant và lấy Entity từ Database
        List<Long> eventIds = qdrantResults.stream()
                .map(result -> {
                    // event_id được lưu dưới dạng Long trong payload
                    return (Long) ((Map<String, Object>) result.get("payload")).get("event_id");
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        // Lấy Entity từ DB bằng ID
        return eventService.getEventsByIds(eventIds); // Giả định EventService có hàm này
    }
}
