package com.group02.openevent.ai.listener;

import com.group02.openevent.ai.qdrant.service.QdrantService;
import com.group02.openevent.ai.service.EmbeddingService;
import com.group02.openevent.model.event.Event;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PostRemove;
import jakarta.persistence.PostUpdate;
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
    @PostPersist
    public void afterEventCreate(Event event) {
        log.info("Event CREATED (ID: {}), starting sync to Qdrant...", event.getId());
        upsertEventVector(event);
    }

    // Tự động chạy SAU KHI một sự kiện được CẬP NHẬT trong DB
    @PostUpdate
    public void afterEventUpdate(Event event) {
        log.info("Event UPDATED (ID: {}), starting sync to Qdrant...", event.getId());
        upsertEventVector(event);
    }
    @PostRemove
    public void afterEventDelete(Event event) {
        log.info("Event DELETED (ID: {}), starting delete from Qdrant...", event.getId());
        try {
            // Lấy ID của sự kiện
            String eventId = String.valueOf(event.getId());

            // Gọi service của Qdrant để xóa
            qdrantService.deleteEmbedding(eventId);

            log.info("✅ Successfully deleted event ID {} from Qdrant.", eventId);
        } catch (Exception e) {
            log.error("❌ Failed to delete event ID {} from Qdrant: {}", event.getId(), e.getMessage());
        }
    }

    private void upsertEventVector(Event event) {
        try {
            float[] vector = embeddingService.getEmbedding(event.getTitle());

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
