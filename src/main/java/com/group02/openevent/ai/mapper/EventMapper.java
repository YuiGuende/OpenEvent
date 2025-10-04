package com.group02.openevent.ai.mapper;

import com.group02.openevent.ai.dto.EventItem;
import com.group02.openevent.model.event.Event;
import com.group02.openevent.model.event.Place;

import java.util.List;
import java.util.stream.Collectors;

public class EventMapper {

    public static EventItem toEventItem(Event event) {
        if (event == null) return null;

        // Lấy place đầu tiên
        String placeName = null;
        if (event.getPlaces() != null && !event.getPlaces().isEmpty()) {
            placeName = event.getPlaces().get(0).getPlaceName();
        }

        // AI tự tính priority
        String priority = calculatePriority(event);

        return new EventItem(
                event.getId(),
                event.getTitle(),       // đúng field trong bảng
                event.getDescription(),
                event.getStartsAt(),
                event.getEndsAt(),
                placeName,
                event.getEnrollDeadline(),
                event.getCreatedAt(),
                event.getEventType(),
                event.getStatus(),
                priority
        );
    }

    private static String calculatePriority(Event event) {
        // Rule đơn giản, sau này có thể dùng AI để phân loại
        if (event.getTitle() != null && event.getTitle().toLowerCase().contains("trường")) {
            return "HIGH";
        }
        return "MEDIUM";
    }
}
