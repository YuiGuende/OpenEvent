package com.group02.openevent.ai.qdrant.service;

import com.group02.openevent.ai.service.EmbeddingService;
import com.group02.openevent.model.enums.Building;
import com.group02.openevent.model.event.Event;
import com.group02.openevent.model.event.Place;
import com.group02.openevent.model.enums.EventStatus;
import com.group02.openevent.model.enums.EventType;
import com.group02.openevent.model.event.Speaker;
import com.group02.openevent.service.EventService;
import com.group02.openevent.service.PlaceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Đồng bộ Event & một số intent mẫu vào Qdrant (vector store)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EventVectorSyncService {

    private final QdrantService qdrantService;     // com.group02.openevent.ai.qdrant.service.QdrantService (của bạn)
    private final EventService eventService;       // com.group02.openevent.service.EventService
    private final EmbeddingService embeddingService; // com.group02.openevent.ai.service.EmbeddingService
    private final PlaceService placeService;

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");


    /**
     * Gộp text từ Event thành 1 đoạn để embed, bao gồm tên địa điểm và tên toà nhà/khu vực.
     */
    private String toSearchableText(Event e) {
        // 1. Xử lý thông tin Diễn giả (Speakers)
        String speakers = (e.getSpeakers() == null) ? "" :
                e.getSpeakers().stream()
                        .map(Speaker::getName)
                        .filter(Objects::nonNull)
                        .collect(Collectors.joining("; "));

        // 2. Xử lý thông tin Địa điểm đã làm giàu (PlaceName + Building)
        String places = (e.getPlaces() == null) ? "" :
                e.getPlaces().stream()
                        .map(p -> {
                            String name = nullSafe(p.getPlaceName());
                            // Xử lý Building Enum (Cần phương thức getBuildingName(p) nếu Building là Enum)
                            String buildingStr = getBuildingName(p);

                            // Gộp lại: "Sảnh Alpha - Tòa Alpha"
                            if (!buildingStr.isEmpty()) {
                                return name + " - Tòa " + buildingStr;
                            }
                            return name;
                        })
                        .filter(s -> !s.isBlank())
                        .collect(Collectors.joining("; "));
                         // Dùng ";" để tách biệt các địa điểm

        // 2. Gộp tất cả các trường quan trọng thành một chuỗi tìm kiếm duy nhất
        return String.join(" ",
                // Nội dung chính
                nullSafe(e.getTitle()),
                nullSafe(e.getDescription()),

                // Nội dung/Lợi ích
                nullSafe(e.getBenefits()),
                nullSafe(e.getLearningObjects()),

                // Thông tin Diễn giả
                "Speakers:", speakers,

                // Thông tin Địa điểm đã làm giàu
                places,

                // Các thuộc tính metadata quan trọng (Capacity & Points)
                e.getCapacity() != null ? "Capacity:" + e.getCapacity().toString() : "",
                e.getPoints() != null ? "Points:" + e.getPoints().toString() : "",

                // Thông tin Thời gian/Trạng thái
                "StartsAt:", e.getStartsAt() != null ? e.getStartsAt().format(TS) : "",
                "EndsAt:", e.getEndsAt() != null ? e.getEndsAt().format(TS) : "",
                "Status:", e.getStatus() != null ? e.getStatus().name() : "",
                "Type:", e.getEventType() != null ? e.getEventType().name() : ""
        ).trim();
    }

    // THÊM PHƯƠNG THỨC MỚI ĐỂ XỬ LÝ ENUM BUILDING
    private String getBuildingName(Place p) {
        if (p.getBuilding() == null) {
            return "";
        }
        String name = p.getBuilding().name();
        if (name.equals(Building.NONE.name())) { // Giả sử Enum Building có NONE
            return ""; // Không thêm "NONE" vào chuỗi tìm kiếm
        }
        return name;
    }

    private String nullSafe(String s) {
        return s == null ? "" : s;
    }
    /**
     * Đồng bộ toàn bộ sự kiện đang có vào Qdrant THEO LÔ.
     */
    @Transactional(readOnly = true)
    public void syncAllEvents() {
        List<Event> allEvents = eventService.getAllEvents();
        if (allEvents.isEmpty()) {
            log.info("Không có sự kiện nào để đồng bộ.");
            return;
        }
        log.info("Tìm thấy {} sự kiện. Bắt đầu đồng bộ theo lô...", allEvents.size());

        try {
            // 1. Chuẩn bị tất cả văn bản cần tạo embedding
            List<String> textsToEmbed = allEvents.stream().map(this::toSearchableText).toList();

            // 2. Gọi EmbeddingService MỘT LẦN DUY NHẤT
            List<float[]> vectors;
            try {
                vectors = embeddingService.getEmbeddings(textsToEmbed);
            } catch (IllegalStateException e) {
                log.warn("Embedding service không khả dụng, bỏ qua sync events: {}", e.getMessage());
                return;
            } catch (Exception e) {
                log.error("Lỗi khi tạo embeddings cho events: {}", e.getMessage());
                return;
            }

            // 3. Chuẩn bị danh sách các điểm (points) để upsert
            List<Map<String, Object>> pointsToUpsert = IntStream.range(0, allEvents.size())
                    .mapToObj(i -> {
                        Event e = allEvents.get(i);
                        Map<String, Object> payload = createEventPayload(e);

                        String uniqueId = UUID.nameUUIDFromBytes(("event_" + e.getId()).getBytes()).toString();

                        Map<String, Object> point = new HashMap<>();
                        point.put("id", uniqueId);
                        point.put("vector", toFloatList(vectors.get(i)));
                        point.put("payload", payload);

                        return point;
                    })
                    .collect(Collectors.toList());

            // 4. Gọi QdrantService MỘT LẦN DUY NHẤT
            qdrantService.upsertPoints(pointsToUpsert);
            log.info("✅ Đồng bộ thành công {} sự kiện vào Qdrant.", allEvents.size());

        } catch (Exception ex) {
            log.error("❌ Lỗi khi đồng bộ sự kiện theo lô: {}", ex.getMessage(), ex);
        }
    }

    /**
     * Seed những prompt “free time” để AI nhận dạng intent nhanh
     */
//    public void seedPromptFreeTime() {
//        List<String> intents = List.of(
//                "Gợi ý khung giờ học môn Toán",
//                "Bạn có thể gợi ý lịch học toán ngày mai, tuần sau được không?",
//                "Tôi muốn biết lúc nào rảnh để học",
//                "Bạn có thể cho tôi biết thời gian trống để lên lịch?",
//                "Tìm khoảng thời gian rảnh trong tuần",
//                "Lên lịch học phù hợp giúp tôi",
//                "Hãy đề xuất giờ học hợp lý"
//        );
//        seedPrompts(intents, "prompt_free_time");
//    }

    /**
     * Seed những prompt “tổng hợp lịch”
     */
    public void seedPromptSummary() {
        List<String> intents = List.of(
                "Tổng hợp sự kiện hôm nay",
                "Sự kiện tuần tới",
                "Tổng hợp sự kiện ngày mai",
                "Có gì trong tuần này?",
                "Sự kiện tuần sau",
                "Tuần tới có gì không?",
                "Lịch trình hôm nay như thế nào?",
                "Cho tôi biết sự kiện hôm nay",
                "sự kiện hot gần đây",
                "sự kiện nổi bật"
        );
        seedBatch(intents, "prompt", "type", "prompt_summary_time", "prompt");
    }

    /**
     * Seed prompt “gửi email trước sự kiện”
     */
    public void seedPromptSendEmail() {
        List<String> intents = List.of(
                "Gửi email cho tôi trước 30 phút sự kiện này bắt đầu",
                "Nhắc tôi qua email trước 40 phút sự kiện",
                "Trước 1 tiếng thì gửi email nhắc nhé",
                "Email nhắc nhở trước 45 phút",
                "Gửi thông báo mail trước khi sự kiện diễn ra",
                "Làm ơn nhắc tôi bằng email khoảng 30-60 phút trước workshop này bắt đầu"
        );
        seedBatch(intents, "prompt", "type", "prompt_send_email", "prompt");

    }

    /**
     * Seed intent “đã là hoạt động ngoài trời” để cảnh báo thời tiết
     */
    public void seedOutdoorActivities() {
        List<String> vocab = List.of(
                "đá bóng", "bơi", "dã ngoại", "leo núi", "chạy bộ", "tennis", "đạp xe",
                "chơi cầu lông", "cắm trại", "đi bộ đường dài", "đi phượt", "đi banahill"
        );
        seedBatch(vocab, "label", "type", "outdoor_activities", "label");
    }

    /**
     * Seed intent “tạo sự kiện”
     */
    public void seedPromptAddEvent() {
        List<String> intents = List.of(
                "Lên sự kiện âm nhạc vào lúc 7h đến 9h",
                "Tạo sự kiện Inno vào lúc 11h đến 13h",
                "Tạo 1 buổi workshop",
                "Tạo Event Thi Hoa Hậu vào ngày mai lúc 5h tại FPT University",
                "Tạo cuộc thi HackAiThon"
        );
        seedBatch(intents, "tool_prompt", "toolName", "ADD_EVENT", "prompt");
    }

    /* ==================== helpers ==================== */

    private void seedPrompts(List<String> prompts, String type) {
        for (String p : prompts) {
            try {
                float[] vec;
                try {
                    vec = embeddingService.getEmbedding(p);
                } catch (IllegalStateException e) {
                    log.warn("Embedding service không khả dụng, bỏ qua seed prompt: {}", e.getMessage());
                    return; // Dừng luôn vì không thể seed
                }
                Map<String, Object> payload = new LinkedHashMap<>();
                payload.put("kind", "prompt");
                payload.put("type", type);
                payload.put("prompt", p);
                qdrantService.upsertEmbedding(UUID.randomUUID().toString(), vec, payload);
            } catch (Exception ex) {
                log.error("Seed prompt failed: {}", ex.getMessage());
            }
        }
    }

    private void seedLabels(List<String> labels, String type) {
        for (String label : labels) {
            try {
                float[] vec;
                try {
                    vec = embeddingService.getEmbedding(label);
                } catch (IllegalStateException e) {
                    log.warn("Embedding service không khả dụng, bỏ qua seed label: {}", e.getMessage());
                    return; // Dừng luôn vì không thể seed
                }
                Map<String, Object> payload = new LinkedHashMap<>();
                payload.put("kind", "label");
                payload.put("type", type);
                payload.put("label", label);
                qdrantService.upsertEmbedding(UUID.randomUUID().toString(), vec, payload);
            } catch (Exception ex) {
                log.error("Seed label failed: {}", ex.getMessage());
            }
        }
    }

    private void seedToolPrompts(List<String> prompts, String toolName) {
        for (String p : prompts) {
            try {
                float[] vec;
                try {
                    vec = embeddingService.getEmbedding(p);
                } catch (IllegalStateException e) {
                    log.warn("Embedding service không khả dụng, bỏ qua seed tool prompt: {}", e.getMessage());
                    return; // Dừng luôn vì không thể seed
                }
                Map<String, Object> payload = new LinkedHashMap<>();
                payload.put("kind", "tool_prompt");
                payload.put("toolName", toolName);
                payload.put("prompt", p);
                qdrantService.upsertEmbedding(UUID.randomUUID().toString(), vec, payload);
            } catch (Exception ex) {
                log.error("Seed tool prompt failed: {}", ex.getMessage());
            }
        }
    }

    /**
     * Seed tất cả các Place vào Qdrant THEO LÔ.
     */
    public void seedAllPlaces() {
        List<Place> allPlaces = placeService.findAllPlaces();
        if (allPlaces.isEmpty()) {
            log.info("Không có địa điểm nào để seed.");
            return;
        }
        log.info("Tìm thấy {} địa điểm. Bắt đầu seed theo lô...", allPlaces.size());

        try {
            // 1. Chuẩn bị texts
            List<String> textsToEmbed = allPlaces.stream()
                    .map(p -> String.join(" ", nullSafe(p.getPlaceName()), getBuildingName(p)).trim())
                    .toList();

            // 2. Tạo embeddings một lần
            List<float[]> vectors;
            try {
                vectors = embeddingService.getEmbeddings(textsToEmbed);
            } catch (IllegalStateException e) {
                // Embedding service không khả dụng, bỏ qua seed
                log.warn("Embedding service không khả dụng, bỏ qua seed places: {}", e.getMessage());
                return;
            } catch (Exception e) {
                log.error("Lỗi khi tạo embeddings cho places: {}", e.getMessage());
                return;
            }

            // 3. Chuẩn bị points
            List<Map<String, Object>> pointsToUpsert = IntStream.range(0, allPlaces.size())
                    .mapToObj(i -> {
                        Place p = allPlaces.get(i);
                        Map<String, Object> payload = new LinkedHashMap<>();
                        payload.put("kind", "place");
                        payload.put("place_id", p.getId());
                        payload.put("name", nullSafe(p.getPlaceName()));
                        payload.put("building", getBuildingName(p));

                        Map<String, Object> point = new HashMap<>();
                        String uniqueId = UUID.nameUUIDFromBytes(("place_" + p.getId()).getBytes()).toString();
                        point.put("id", uniqueId);
                        point.put("vector", toFloatList(vectors.get(i)));
                        point.put("payload", payload);

                        return point;
                    })
                    .collect(Collectors.toList());

            // 4. Upsert một lần
            qdrantService.upsertPoints(pointsToUpsert);
            log.info("✅ Seed thành công {} địa điểm vào Qdrant.", allPlaces.size());

        } catch (Exception ex) {
            log.error("❌ Lỗi khi seed địa điểm theo lô: {}", ex.getMessage(), ex);
        }
    }

    /**
     * ✅ PHƯƠNG THỨC CHUNG ĐỂ SEED THEO LÔ, TRÁNH LẶP CODE
     */
    private void seedBatch(List<String> contents, String kind, String typeKey, String typeValue, String contentKey) {
        if (contents == null || contents.isEmpty()) return;
        log.info("Bắt đầu seed {} items cho kind '{}', type '{}'...", contents.size(), kind, typeValue);

        try {
            // 1. Tạo tất cả embeddings một lần
            List<float[]> vectors;
            try {
                vectors = embeddingService.getEmbeddings(contents);
            } catch (IllegalStateException e) {
                log.warn("Embedding service không khả dụng, bỏ qua seed: {}", e.getMessage());
                return;
            } catch (Exception e) {
                log.error("Lỗi khi tạo embeddings: {}", e.getMessage());
                return;
            }

            // 2. Chuẩn bị points
            List<Map<String, Object>> pointsToUpsert = IntStream.range(0, contents.size())
                    .mapToObj(i -> {
                        Map<String, Object> payload = new LinkedHashMap<>();
                        payload.put("kind", kind);
                        payload.put(typeKey, typeValue);
                        payload.put(contentKey, contents.get(i));

                        Map<String, Object> point = new HashMap<>();
                        point.put("id", UUID.randomUUID().toString());
                        point.put("vector", toFloatList(vectors.get(i)));
                        point.put("payload", payload);

                        return point;
                    })
                    .collect(Collectors.toList());

            // 3. Upsert tất cả points một lần
            qdrantService.upsertPoints(pointsToUpsert);
            log.info("✅ Seed thành công {} items.", contents.size());
        } catch (Exception ex) {
            log.error("❌ Lỗi khi seed theo lô cho kind '{}', type '{}': {}", kind, typeValue, ex.getMessage());
        }
    }
    // Helper để chuyển float[] sang List<Float>
    private List<Float> toFloatList(float[] vector) {
        if (vector == null) return Collections.emptyList();
        return IntStream.range(0, vector.length)
                .mapToObj(i -> vector[i])
                .collect(Collectors.toList());
    }
    /**
     * Helper để tạo payload đầy đủ cho một đối tượng Event.
     * Tách ra từ hàm syncAllEvents cũ để tái sử dụng và làm sạch code.
     * @param e Đối tượng Event cần tạo payload.
     * @return Một Map chứa tất cả các metadata cần thiết cho Qdrant.
     */
    private Map<String, Object> createEventPayload(Event e) {
        Map<String, Object> payload = new LinkedHashMap<>();

        // --- Thông tin cơ bản ---
        payload.put("kind", "event");
        payload.put("event_id", e.getId());
        payload.put("title", nullSafe(e.getTitle()));
        payload.put("description", nullSafe(e.getDescription()));

        // --- Thời gian (định dạng thành chuỗi) ---
        // Ghi chú: Lưu dưới dạng Unix timestamp (số nguyên) sẽ tốt hơn cho việc lọc theo khoảng thời gian
        // Ví dụ: payload.put("startsAt_timestamp", e.getStartsAt() != null ? e.getStartsAt().toEpochSecond(java.time.ZoneOffset.UTC) : null);
        payload.put("startsAt", e.getStartsAt() != null ? e.getStartsAt().format(TS) : null);
        payload.put("endsAt", e.getEndsAt() != null ? e.getEndsAt().format(TS) : null);
        payload.put("enrollDeadline", e.getEnrollDeadline() != null ? e.getEnrollDeadline().format(TS) : null);
        payload.put("publicDate", e.getPublicDate() != null ? e.getPublicDate().format(TS) : null);

        // --- Phân loại và Trạng thái ---
        payload.put("status", e.getStatus() != null ? e.getStatus().name() : null);
        payload.put("eventType", e.getEventType() != null ? e.getEventType().name() : null); // Thay vì dùng OTHERS, để null nếu không có

        // --- Thông tin tổ chức ---
        payload.put("orgName", e.getOrganization() != null ? e.getOrganization().getOrgName() : null);
        payload.put("hostName", e.getHost() != null ? e.getHost().getHostName() : null);

        // --- Thuộc tính khác ---
        payload.put("capacity", e.getCapacity());
        payload.put("points", e.getPoints());
        payload.put("benefits", e.getBenefits());
        payload.put("learningObjects", e.getLearningObjects());

        // --- Dữ liệu từ các mối quan hệ (Relationships) ---
        payload.put("speakers",
                e.getSpeakers() == null ? Collections.emptyList()
                        : e.getSpeakers().stream().map(Speaker::getName).filter(Objects::nonNull).toList()
        );

        payload.put("places",
                e.getPlaces() == null ? Collections.emptyList()
                        : e.getPlaces().stream().map(Place::getPlaceName).filter(Objects::nonNull).toList()
        );

        // --- Thông tin giá vé ---
        payload.put("minPrice", e.getMinTicketPice());
        payload.put("maxPrice", e.getMaxTicketPice());

        return payload;
    }
}
