package com.group02.openevent.scheduler;

import com.group02.openevent.model.event.Event;
import com.group02.openevent.model.enums.EventStatus;
import com.group02.openevent.repository.IEventRepo;
import com.group02.openevent.service.EventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Scheduler để tự động cập nhật status của các sự kiện thành ONGOING
 * khi thời gian bắt đầu (startsAt) <= hôm nay và chưa kết thúc
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EventStatusUpdateScheduler {
    
    private final IEventRepo eventRepo;
    private final EventService eventService;
    
    @Value("${event.auto-update-status-enabled:true}")
    private boolean autoUpdateEnabled;
    
    /**
     * Tự động cập nhật status các sự kiện thành ONGOING
     * Chạy mỗi giờ (3600 giây)
     */
    @Scheduled(fixedRate = 3600000) // 1 hour = 3,600,000 milliseconds
    @Transactional
    public void updateEventStatusToOngoing() {
        if (!autoUpdateEnabled) {
            log.debug("Auto-update event status is disabled");
            return;
        }
        
        try {
            log.info("🔄 Checking for events to update to ONGOING status...");
            
            LocalDateTime now = LocalDateTime.now();
            log.debug("Current time: {}", now);
            
            // Tìm các sự kiện cần cập nhật status
            List<Event> eventsToUpdate = eventRepo.findEventsToUpdateToOngoing(now);
            
            if (eventsToUpdate.isEmpty()) {
                log.debug("No events found to update to ONGOING status");
                return;
            }
            
            log.info("Found {} events to update to ONGOING status", eventsToUpdate.size());
            
            int updatedCount = 0;
            for (Event event : eventsToUpdate) {
                try {
                    // Kiểm tra lại status để tránh race condition
                    if (event.getStatus() == EventStatus.ONGOING || 
                        event.getStatus() == EventStatus.FINISH || 
                        event.getStatus() == EventStatus.CANCEL) {
                        log.debug("Event {} (ID: {}) status is already {}, skipping", 
                                event.getTitle(), event.getId(), event.getStatus());
                        continue;
                    }
                    
                    // Kiểm tra lại điều kiện thời gian
                    if (event.getStartsAt() == null || event.getStartsAt().isAfter(now)) {
                        log.debug("Event {} (ID: {}) hasn't started yet, skipping", 
                                event.getTitle(), event.getId());
                        continue;
                    }
                    
                    if (event.getEndsAt() == null || event.getEndsAt().isBefore(now) || 
                        event.getEndsAt().isEqual(now)) {
                        log.debug("Event {} (ID: {}) has already ended, skipping", 
                                event.getTitle(), event.getId());
                        continue;
                    }
                    
                    // Cập nhật status thành ONGOING
                    EventStatus oldStatus = event.getStatus();
                    eventService.updateEventStatus(event.getId(), EventStatus.ONGOING);
                    updatedCount++;
                    
                    log.info("✅ Updated event '{}' (ID: {}) status from {} to ONGOING", 
                            event.getTitle(), event.getId(), oldStatus);
                    
                } catch (Exception e) {
                    log.error("❌ Error updating event {} (ID: {}): {}", 
                            event.getTitle(), event.getId(), e.getMessage(), e);
                }
            }
            
            if (updatedCount > 0) {
                log.info("✅ Successfully updated {} events to ONGOING status", updatedCount);
            } else {
                log.debug("No events were updated (all were already processed or didn't meet criteria)");
            }
            
        } catch (Exception e) {
            log.error("❌ Error in event status update scheduler: {}", e.getMessage(), e);
        }
    }
}

