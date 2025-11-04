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
        return savedImage;
    }

    @Override
    @Transactional
    public EventImage update(EventImage image) {
        return eventImageRepo.save(image);
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        eventImageRepo.deleteById(id);
    }

    @Override
    @Transactional
    public void setMainPoster(Long imageId) {
        EventImage image = findById(imageId);
        if (image == null) {
            throw new RuntimeException("Image not found with id: " + imageId);
        }

        // Unset all other main posters for this event
        List<EventImage> eventImages = eventImageRepo.findByEventId(image.getEvent().getId());
        for (EventImage eventImage : eventImages) {
            eventImage.setMainPoster(false);
            eventImageRepo.save(eventImage);
        }

        // Set this image as main poster
        image.setMainPoster(true);
        eventImageRepo.save(image);
    }
}
