package com.group02.openevent.controller.event;

import com.group02.openevent.model.event.Event;
import com.group02.openevent.model.event.EventSchedule;
import com.group02.openevent.service.EventScheduleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/api/schedules")
public class ScheduleController {

    @Autowired
    private EventScheduleService scheduleService;

    @GetMapping("/event/{eventId}")
    @ResponseBody
    public ResponseEntity<List<EventSchedule>> getSchedulesByEventId(@PathVariable Long eventId) {
        List<EventSchedule> schedules = scheduleService.findByEventId(eventId);
        return ResponseEntity.ok(schedules);
    }

    @PostMapping
    @ResponseBody
    public ResponseEntity<?> createSchedule(@RequestBody Map<String, Object> requestData) {
        try {
            // Extract data from request
            String activity = (String) requestData.get("activity");
            String startTime = (String) requestData.get("startTime");
            String endTime = (String) requestData.get("endTime");
            String description = (String) requestData.get("description");
            Long eventId = Long.valueOf(requestData.get("eventId").toString());
            
            // Validate required fields
            if (activity == null || activity.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Activity title is required"));
            }

            if (startTime == null || endTime == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Start time and end time are required"));
            }

            // Create EventSchedule object
            EventSchedule schedule = new EventSchedule();
            schedule.setActivity(activity);
            schedule.setStartTime(java.time.LocalDateTime.parse(startTime));
            schedule.setEndTime(java.time.LocalDateTime.parse(endTime));
            schedule.setDescription(description);
            
            // Set event by ID
            Event event = new Event();
            event.setId(eventId);
            schedule.setEvent(event);

            EventSchedule createdSchedule = scheduleService.create(schedule);
            return ResponseEntity.ok(createdSchedule);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("error", "Failed to create schedule: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    @ResponseBody
    public ResponseEntity<?> updateSchedule(@PathVariable Long id, @RequestBody EventSchedule schedule) {
        try {
            EventSchedule updatedSchedule = scheduleService.update(id, schedule);
            return ResponseEntity.ok(updatedSchedule);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("error", "Failed to update schedule: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSchedule(@PathVariable Long id) {
        try {
            scheduleService.deleteById(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().build();
        }
    }
}