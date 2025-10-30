package com.group02.openevent.controller.api;

import com.group02.openevent.dto.response.DashboardStatsResponse;
import com.group02.openevent.service.DashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardApiController {
    
    private final DashboardService dashboardService;
    
    @GetMapping("/event/{eventId}/stats")
    public ResponseEntity<DashboardStatsResponse> getEventDashboardStats(@PathVariable Long eventId) {
        log.info("API: Getting dashboard stats for event ID: {}", eventId);
        
        try {
            DashboardStatsResponse stats = dashboardService.getEventDashboardStats(eventId);
            log.info("API: Successfully retrieved dashboard stats for event {}", eventId);
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("API: Error getting dashboard stats for event {}: {}", eventId, e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
}
