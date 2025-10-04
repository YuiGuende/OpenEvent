package com.group02.openevent.ai.controller;

import com.group02.openevent.ai.dto.Action;
import com.group02.openevent.ai.dto.TimeSlot;
import com.group02.openevent.ai.service.AgentEventService;
import com.group02.openevent.model.event.Event;
import com.group02.openevent.service.EventService;
import com.group02.openevent.service.PlaceService;
import com.group02.openevent.util.TimeSlotUnit;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Controller để xử lý các chức năng AI liên quan đến Event
 * @author Admin
 */
@RestController
@RequestMapping("/api/ai/event")
@CrossOrigin(origins = "*")
@Tag(name = "Event AI Controller", description = "API for AI-powered event operations")
public class EventAIController {

    private final AgentEventService agentEventService;
    private final EventService eventService;
    private final PlaceService placeService;

    public EventAIController(AgentEventService agentEventService,
                           EventService eventService,
                           PlaceService placeService) {
        this.agentEventService = agentEventService;
        this.eventService = eventService;
        this.placeService = placeService;
    }

    /**
     * Thực hiện action tạo event từ AI
     * @param action Action từ AI
     * @return ResponseEntity chứa kết quả
     */
    @PostMapping("/create")
    public ResponseEntity<Map<String, Object>> createEvent(@RequestBody Action action) {
        try {
            if (!"ADD_EVENT".equals(action.getToolName())) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "❌ Chỉ hỗ trợ action ADD_EVENT"));
            }
            
            agentEventService.saveEventFromAction(action);
            
            Map<String, Object> result = Map.of(
                "success", true,
                "message", "✅ Đã tạo sự kiện thành công",
                "eventTitle", action.getArgs().get("title")
            );
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> result = Map.of(
                "success", false,
                "error", "❌ Lỗi khi tạo sự kiện: " + e.getMessage()
            );
            return ResponseEntity.internalServerError().body(result);
        }
    }

    /**
     * Thực hiện action cập nhật event từ AI
     * @param action Action từ AI
     * @return ResponseEntity chứa kết quả
     */
    @PostMapping("/update")
    public ResponseEntity<Map<String, Object>> updateEvent(@RequestBody Action action) {
        try {
            if (!"UPDATE_EVENT".equals(action.getToolName())) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "❌ Chỉ hỗ trợ action UPDATE_EVENT"));
            }
            
            agentEventService.updateEventFromAction(action);
            
            Map<String, Object> result = Map.of(
                "success", true,
                "message", "✅ Đã cập nhật sự kiện thành công"
            );
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> result = Map.of(
                "success", false,
                "error", "❌ Lỗi khi cập nhật sự kiện: " + e.getMessage()
            );
            return ResponseEntity.internalServerError().body(result);
        }
    }

    /**
     * Thực hiện action xóa event từ AI
     * @param action Action từ AI
     * @return ResponseEntity chứa kết quả
     */
    @PostMapping("/delete")
    public ResponseEntity<Map<String, Object>> deleteEvent(@RequestBody Action action) {
        try {
            if (!"DELETE_EVENT".equals(action.getToolName())) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "❌ Chỉ hỗ trợ action DELETE_EVENT"));
            }
            
            agentEventService.deleteEventFromAction(action);
            
            Map<String, Object> result = Map.of(
                "success", true,
                "message", "✅ Đã xóa sự kiện thành công"
            );
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> result = Map.of(
                "success", false,
                "error", "❌ Lỗi khi xóa sự kiện: " + e.getMessage()
            );
            return ResponseEntity.internalServerError().body(result);
        }
    }

    /**
     * Lấy danh sách thời gian rảnh
     * @param request Map chứa thông tin yêu cầu
     * @return ResponseEntity chứa danh sách thời gian rảnh
     */
    @PostMapping("/free-time")
    public ResponseEntity<Map<String, Object>> getFreeTime(@RequestBody Map<String, Object> request) {
        try {
            String timeContext = (String) request.getOrDefault("timeContext", "THIS_WEEK");
            String placeName = (String) request.getOrDefault("place", "");
            
            List<Event> events;
            if (!placeName.isEmpty()) {
                Optional<com.group02.openevent.model.event.Place> placeOpt = 
                    placeService.findPlaceByName(placeName);
                if (placeOpt.isPresent()) {
                    events = eventService.getEventsByPlace(placeOpt.get().getId());
                } else {
                    return ResponseEntity.badRequest()
                        .body(Map.of("error", "❌ Không tìm thấy địa điểm: " + placeName));
                }
            } else {
                events = eventService.getAllEvents();
            }
            
            // Lọc theo ngữ cảnh thời gian
            List<Event> filteredEvents;
            switch (timeContext) {
                case "TODAY" -> filteredEvents = TimeSlotUnit.filterEventsToday(events);
                case "TOMORROW" -> filteredEvents = TimeSlotUnit.filterEventsTomorrow(events);
                case "THIS_WEEK" -> filteredEvents = TimeSlotUnit.filterEventsThisWeek(events);
                case "NEXT_WEEK" -> filteredEvents = TimeSlotUnit.filterEventsNextWeek(events);
                default -> filteredEvents = events;
            }
            
            List<TimeSlot> freeSlots = TimeSlotUnit.findFreeTime(filteredEvents);
            
            Map<String, Object> result = Map.of(
                "success", true,
                "freeSlots", freeSlots,
                "timeContext", timeContext,
                "place", placeName,
                "totalFreeSlots", freeSlots.size()
            );
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> result = Map.of(
                "success", false,
                "error", "❌ Lỗi khi lấy thời gian rảnh: " + e.getMessage()
            );
            return ResponseEntity.internalServerError().body(result);
        }
    }

    /**
     * Kiểm tra xung đột thời gian cho event
     * @param request Map chứa thông tin event
     * @return ResponseEntity chứa kết quả kiểm tra
     */
    @PostMapping("/check-conflict")
    public ResponseEntity<Map<String, Object>> checkTimeConflict(@RequestBody Map<String, Object> request) {
        try {
            LocalDateTime startTime = LocalDateTime.parse((String) request.get("startTime"));
            LocalDateTime endTime = LocalDateTime.parse((String) request.get("endTime"));
            String placeName = (String) request.get("place");
            
            List<com.group02.openevent.model.event.Place> places = List.of();
            if (placeName != null && !placeName.isEmpty()) {
                Optional<com.group02.openevent.model.event.Place> placeOpt = 
                    placeService.findPlaceByName(placeName);
                if (placeOpt.isPresent()) {
                    places = List.of(placeOpt.get());
                }
            }
            
            List<Event> conflicts = eventService.isTimeConflict(startTime, endTime, places);
            
            Map<String, Object> result = new HashMap<>();
            result.put("hasConflict", !conflicts.isEmpty());
            result.put("conflicts", conflicts);
            result.put("conflictCount", conflicts.size());
            
            if (!conflicts.isEmpty()) {
                result.put("message", "⚠️ Phát hiện " + conflicts.size() + " xung đột thời gian");
            } else {
                result.put("message", "✅ Không có xung đột thời gian");
            }
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> result = Map.of(
                "hasConflict", false,
                "error", "❌ Lỗi khi kiểm tra xung đột: " + e.getMessage()
            );
            return ResponseEntity.internalServerError().body(result);
        }
    }

    /**
     * Lấy thống kê events
     * @return ResponseEntity chứa thống kê
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getEventStats() {
        try {
            List<Event> allEvents = eventService.getAllEvents();
            
            long totalEvents = allEvents.size();
            long todayEvents = TimeSlotUnit.filterEventsToday(allEvents).size();
            long tomorrowEvents = TimeSlotUnit.filterEventsTomorrow(allEvents).size();
            long thisWeekEvents = TimeSlotUnit.filterEventsThisWeek(allEvents).size();
            
            Map<String, Object> stats = Map.of(
                "totalEvents", totalEvents,
                "todayEvents", todayEvents,
                "tomorrowEvents", tomorrowEvents,
                "thisWeekEvents", thisWeekEvents,
                "lastUpdated", LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
            );
            
            return ResponseEntity.ok(stats);
            
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> result = Map.of("error", "❌ Lỗi khi lấy thống kê: " + e.getMessage());
            return ResponseEntity.internalServerError().body(result);
        }
    }
}
