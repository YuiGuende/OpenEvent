package com.group02.openevent.service.impl;

import com.group02.openevent.model.event.Event;
import com.group02.openevent.model.event.EventImage;
import com.group02.openevent.repository.IEventImageRepo;
import com.group02.openevent.repository.IEventRepo;
import com.group02.openevent.service.EventImageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
public class EventImageServiceImpl implements EventImageService {

    @Autowired
    private IEventImageRepo eventImageRepo;

    @Autowired
    private IEventRepo eventRepo;

    @Override
    public List<EventImage> findByEventId(Long eventId) {
        return eventImageRepo.findByEventId(eventId);
    }

    @Override
    public EventImage findById(Long id) {
        return eventImageRepo.findById(id).orElse(null);
    }

    @Override
    @Transactional
    public EventImage create(EventImage image, Long eventId) {
        log.info("=== EventImageServiceImpl.create CALLED ===");
        log.info("Creating image: url={}, orderIndex={}, isMainPoster={}, eventId={}", 
                image.getUrl(), image.getOrderIndex(), image.isMainPoster(), eventId);
        
        // Load event from database
        Event event = eventRepo.findById(eventId).orElse(null);
        if (event == null) {
            log.error("Event not found with id: {}", eventId);
            throw new RuntimeException("Event not found with id: " + eventId);
        }

        log.info("Event found: id={}, title={}", event.getId(), event.getTitle());

        // Set the event
        image.setEvent(event);

        // KHÔNG unset mainPoster khác - cho phép nhiều poster cùng có mainPoster = true
        // Tất cả ảnh từ nút "Thêm poster" đều có mainPoster = true

        EventImage savedImage = eventImageRepo.save(image);
        log.info("Image saved successfully with ID: {}", savedImage.getId());
        
        // Update event imageUrl if this is the main poster (mainPoster = true and orderIndex = 0)
        updateEventImageUrl(eventId);
        
        return savedImage;
    }

    @Override
    @Transactional
    public EventImage update(EventImage image) {
        EventImage updatedImage = eventImageRepo.save(image);
        // Update event imageUrl after updating image
        if (image.getEvent() != null && image.getEvent().getId() != null) {
            updateEventImageUrl(image.getEvent().getId());
        }
        return updatedImage;
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        EventImage image = findById(id);
        Long eventId = null;
        if (image != null && image.getEvent() != null) {
            eventId = image.getEvent().getId();
        }
        eventImageRepo.deleteById(id);
        // Update event imageUrl after deleting image
        if (eventId != null) {
            updateEventImageUrl(eventId);
        }
    }

    @Override
    @Transactional
    public void setMainPoster(Long imageId) {
        EventImage image = findById(imageId);
        if (image == null) {
            throw new RuntimeException("Image not found with id: " + imageId);
        }

        Long eventId = image.getEvent().getId();

        // Unset all other main posters for this event
        List<EventImage> eventImages = eventImageRepo.findByEventId(eventId);
        for (EventImage eventImage : eventImages) {
            eventImage.setMainPoster(false);
            eventImageRepo.save(eventImage);
        }

        // Set this image as main poster
        image.setMainPoster(true);
        eventImageRepo.save(image);
        
        // Update event imageUrl after setting main poster
        updateEventImageUrl(eventId);
    }

    /**
     * Update event imageUrl from the poster with mainPoster = true and orderIndex = 0
     */
    @Transactional
    private void updateEventImageUrl(Long eventId) {
        log.info("Updating event imageUrl for eventId: {}", eventId);
        
        // Find all main posters for this event
        List<EventImage> mainPosters = eventImageRepo.findByEventIdAndMainPoster(eventId, true);
        
        // Find the poster with mainPoster = true and orderIndex = 0
        EventImage mainPoster = mainPosters.stream()
                .filter(img -> img.isMainPoster() && img.getOrderIndex() == 0)
                .findFirst()
                .orElse(null);
        
        if (mainPoster != null) {
            Event event = eventRepo.findById(eventId).orElse(null);
            if (event != null) {
                event.setImageUrl(mainPoster.getUrl());
                eventRepo.save(event);
                log.info("Updated event imageUrl to: {} for eventId: {}", mainPoster.getUrl(), eventId);
            } else {
                log.warn("Event not found with id: {}", eventId);
            }
        } else {
            log.info("No main poster with orderIndex = 0 found for eventId: {}", eventId);
            // Optionally, you could set imageUrl to null or the first main poster
            // For now, we'll leave it as is
        }
    }
}
