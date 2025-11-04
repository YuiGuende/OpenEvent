package com.group02.openevent.controller.event;

import com.group02.openevent.model.event.EventImage;
import com.group02.openevent.service.EventImageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/event-images")
public class EventImageController {

    @Autowired
    private EventImageService eventImageService;

    // Get all images for an event
    @GetMapping("/event/{eventId}")
    public ResponseEntity<List<EventImage>> getImagesByEventId(@PathVariable Long eventId) {
        try {
            List<EventImage> images = eventImageService.findByEventId(eventId);
            return ResponseEntity.ok(images);
        } catch (Exception e) {
            log.error("Error getting images for event {}: {}", eventId, e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    // Create new image
    @PostMapping
    public ResponseEntity<EventImage> createImage(@RequestBody Map<String, Object> imageData) {
        log.info("=== EventImageController.createImage CALLED ===");
        log.info("Received imageData: {}", imageData);
        
        try {
            String url = (String) imageData.get("url");
            Integer orderIndex = (Integer) imageData.get("orderIndex");
            Boolean isMainPoster = (Boolean) imageData.get("isMainPoster");
            Long eventId = null;
            
            // Handle eventId conversion safely
            Object eventIdObj = imageData.get("eventId");
            if (eventIdObj != null) {
                if (eventIdObj instanceof Number) {
                    eventId = ((Number) eventIdObj).longValue();
                } else {
                    eventId = Long.valueOf(eventIdObj.toString());
                }
            }

            log.info("Parsed values - url: {}, orderIndex: {}, isMainPoster: {}, eventId: {}", 
                    url, orderIndex, isMainPoster, eventId);

            if (url == null || url.isEmpty()) {
                log.error("URL is null or empty");
                return ResponseEntity.badRequest().build();
            }
            
            if (eventId == null) {
                log.error("EventId is null");
                return ResponseEntity.badRequest().build();
            }

            EventImage image = new EventImage();
            image.setUrl(url);
            image.setOrderIndex(orderIndex != null ? orderIndex : 0);
            image.setMainPoster(isMainPoster != null ? isMainPoster : false);

            log.info("Creating EventImage: url={}, orderIndex={}, isMainPoster={}, eventId={}", 
                    url, image.getOrderIndex(), image.isMainPoster(), eventId);

            EventImage createdImage = eventImageService.create(image, eventId);
            
            log.info("Image created successfully with ID: {}", createdImage.getId());
            return ResponseEntity.ok(createdImage);
        } catch (NumberFormatException e) {
            log.error("Error parsing eventId: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        } catch (RuntimeException e) {
            log.error("Runtime error creating image: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Error creating image: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }

    // Update image (mainly for setting main poster)
    @PutMapping("/{id}")
    public ResponseEntity<EventImage> updateImage(@PathVariable Long id, @RequestBody Map<String, Object> imageData) {
        try {
            EventImage existingImage = eventImageService.findById(id);
            if (existingImage == null) {
                return ResponseEntity.notFound().build();
            }

            // Update mainPoster status
            Boolean isMainPoster = (Boolean) imageData.get("isMainPoster");
            if (isMainPoster != null) {
                existingImage.setMainPoster(isMainPoster);
            }

            EventImage updatedImage = eventImageService.update(existingImage);
            return ResponseEntity.ok(updatedImage);
        } catch (Exception e) {
            log.error("Error updating image {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    // Delete image
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteImage(@PathVariable Long id) {
        try {
            eventImageService.deleteById(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error deleting image {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    // Set main poster (automatically unset others)
    @PutMapping("/{id}/set-main")
    public ResponseEntity<Void> setMainPoster(@PathVariable Long id) {
        try {
            eventImageService.setMainPoster(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error setting main poster {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
}
