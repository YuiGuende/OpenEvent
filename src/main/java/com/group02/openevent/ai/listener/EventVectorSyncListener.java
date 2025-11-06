package com.group02.openevent.ai.listener;

import com.group02.openevent.ai.qdrant.service.QdrantService;
import com.group02.openevent.ai.service.EmbeddingService;
import com.group02.openevent.model.event.Event;
// import jakarta.persistence.PostPersist; // Commented out - not used
// import jakarta.persistence.PostUpdate; // Commented out - not used
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
public class EventVectorSyncListener {

    // Sử dụng static để inject dependency vào một listener
    private static EmbeddingService embeddingService;
    private static QdrantService qdrantService;

    @Autowired
    public void init(EmbeddingService embeddingService, QdrantService qdrantService) {
        EventVectorSyncListener.embeddingService = embeddingService;
        EventVectorSyncListener.qdrantService = qdrantService;
    }

    // Tự động chạy SAU KHI một sự kiện MỚI được lưu vào DB
    // @PostPersist - Commented out to prevent errors when embedding service is not available
    // public void afterEventCreate(Event event) {
    //     log.info("Event CREATED (ID: {}), starting sync to Qdrant...", event.getId());
    //     upsertEventVector(event);
    // }

    // Tự động chạy SAU KHI một sự kiện được CẬP NHẬT trong DB
    // @PostUpdate - Commented out to prevent errors when embedding service is not available
    // public void afterEventUpdate(Event event) {
    //     log.info("Event UPDATED (ID: {}), starting sync to Qdrant...", event.getId());
    //     upsertEventVector(event);
    // }

    private void upsertEventVector(Event event) {
        try {
            float[] vector;
            try {
                vector = embeddingService.getEmbedding(event.getTitle());
            } catch (IllegalStateException e) {
                // Embedding service không khả dụng, bỏ qua sync
                log.warn("Embedding service không khả dụng, bỏ qua sync event '{}' to Qdrant: {}", 
                        event.getTitle(), e.getMessage());
                return;
            } catch (Exception e) {
                log.error("❌ Lỗi khi tạo embedding cho event ID {}: {}", event.getId(), e.getMessage());
                return;
            }

            Map<String, Object> payload = new HashMap<>();
            payload.put("event_id", event.getId());
            payload.put("title", event.getTitle());
            payload.put("kind", "event");
            payload.put("startsAt", event.getStartsAt().toEpochSecond(java.time.ZoneOffset.UTC));

            qdrantService.upsertEmbedding(String.valueOf(event.getId()), vector, payload);
            log.info("✅ Successfully synced event '{}' to Qdrant.", event.getTitle());
        } catch (Exception e) {
            log.error("❌ Failed to sync event ID {} to Qdrant: {}", event.getId(), e.getMessage());
        }
    }
}
