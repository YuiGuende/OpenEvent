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
     * Đồng bộ toàn bộ sự kiện đang có vào Qdrant
     */
    @Transactional
    public void syncAllEvents() {
        List<Event> all = eventService.getAllEvents();
        log.info("Found {} events to sync", all.size());

        for (Event e : all) {
            try {
                String content = toSearchableText(e);
                float[] vec = embeddingService.getEmbedding(content);

                Map<String, Object> payload = new LinkedHashMap<>();
                payload.put("kind", "event");
                payload.put("event_id", e.getId());
                payload.put("title", nullSafe(e.getTitle()));
                payload.put("description", nullSafe(e.getDescription()));
                payload.put("startsAt", e.getStartsAt() != null ? e.getStartsAt().format(TS) : null);
                payload.put("endsAt", e.getEndsAt() != null ? e.getEndsAt().format(TS) : null);
                payload.put("enrollDeadline", e.getEnrollDeadline() != null ? e.getEnrollDeadline().format(TS) : null);
                payload.put("status", e.getStatus() != null ? e.getStatus().name() : null);
                payload.put("eventType", e.getEventType() != null ? e.getEventType().name() : EventType.OTHERS.name());
                payload.put("orgName", e.getOrganization() != null ? e.getOrganization().getOrgName() : null);
                payload.put("hostName", e.getHost() != null ? e.getHost().getHostName() : null);
                payload.put("capacity", e.getCapacity());
                payload.put("points", e.getPoints());
                payload.put("publicDate", e.getPublicDate() != null ? e.getPublicDate().format(TS) : null);
                payload.put("benefits", e.getBenefits());
                payload.put("learningObjects", e.getLearningObjects());

// Thêm thông tin diễn giả (tên)
                payload.put("speakers",
                        e.getSpeakers() == null ? List.of()
                                : e.getSpeakers().stream().map(Speaker::getName).filter(Objects::nonNull).toList()
                );

// Thêm thông tin giá vé (Min/Max Price)
// Lưu ý: Đảm bảo TicketTypeDTO được xử lý đúng cách, nếu không, dùng getter trực tiếp
                payload.put("minPrice", e.getMinTicketPice());
                payload.put("maxPrice", e.getMaxTicketPice());
// END: Thêm các trường bị thiếu/quan trọng
                payload.put("places",
                        e.getPlaces() == null ? List.of()
                                : e.getPlaces().stream().map(Place::getPlaceName).filter(Objects::nonNull).toList()
                );

                // dùng event id làm point id (Qdrant chấp nhận integer id)
                String id = String.valueOf(e.getId());

                qdrantService.upsertEmbedding(id, vec, payload);
                log.info("Upserted event {} (id={})", e.getTitle(), e.getId());
            } catch (Exception ex) {
                log.error("Sync event id={} failed: {}", e.getId(), ex.getMessage());
            }
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
                "Cho tôi biết sự kiện hôm nay"
        );
        seedPrompts(intents, "prompt_summary_time");
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
        seedPrompts(intents, "prompt_send_email");
    }

    /**
     * Seed intent “đã là hoạt động ngoài trời” để cảnh báo thời tiết
     */
    public void seedOutdoorActivities() {
        List<String> vocab = List.of(
                "đá bóng", "bơi", "dã ngoại", "leo núi", "chạy bộ", "tennis", "đạp xe",
                "chơi cầu lông", "cắm trại", "đi bộ đường dài", "đi phượt", "đi banahill"
        );
        seedLabels(vocab, "outdoor_activities");
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
        seedToolPrompts(intents, "ADD_EVENT");
    }

    /* ==================== helpers ==================== */

    private void seedPrompts(List<String> prompts, String type) {
        for (String p : prompts) {
            try {
                float[] vec = embeddingService.getEmbedding(p);
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
                float[] vec = embeddingService.getEmbedding(label);
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
                float[] vec = embeddingService.getEmbedding(p);
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
     * Seed tất cả các Place hiện có trong DB vào Qdrant để hỗ trợ tìm kiếm thực thể (entity search).
     */
    public void seedAllPlaces() {
        // Giả định PlaceService đã được inject và có phương thức getAllPlaces()
        List<Place> allPlaces = placeService.findAllPlaces();
        // Thay thế bằng logic logging của bạn nếu không dùng log.info
        System.out.println("Found " + allPlaces.size() + " places to seed into Qdrant.");

        for (Place p : allPlaces) {
            try {
                // Chuỗi văn bản dùng để tạo vector (chỉ dùng các trường hiện có)
                String content = String.join(" ",
                        nullSafe(p.getPlaceName()),
                        p.getBuilding() != Building.NONE ? "Tòa " + p.getBuilding().name() : ""
                ).trim();

                if (content.isEmpty()) {
                    System.out.println("Skipping place ID=" + p.getId() + " due to empty content.");
                    continue;
                }

                float[] vec = embeddingService.getEmbedding(content);

                Map<String, Object> payload = new LinkedHashMap<>();
                payload.put("kind", "place"); // KEY QUAN TRỌNG NHẤT CHO FILTERING
                payload.put("place_id", p.getId());
                payload.put("name", nullSafe(p.getPlaceName()));
                payload.put("building", p.getBuilding() != Building.NONE ? p.getBuilding().name() : "");

                // Dùng Place ID làm Point ID (đảm bảo nó là String)
                String id = String.valueOf(p.getId());

                qdrantService.upsertEmbedding(id, vec, payload);
                System.out.println("Upserted place: " + p.getPlaceName() + " (ID=" + p.getId() + ")");

            } catch (Exception ex) {
                System.err.println("Seed place ID=" + p.getId() + " failed: " + ex.getMessage());
            }
        }
    }

}
