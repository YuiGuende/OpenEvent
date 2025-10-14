package com.group02.openevent.service;

import com.group02.openevent.model.event.EventImage;

import java.util.List;

public interface EventImageService {
    List<EventImage> findByEventId(Long eventId);
    EventImage findById(Long id);
    EventImage create(EventImage image, Long eventId);
    EventImage update(EventImage image);
    void deleteById(Long id);
    void setMainPoster(Long imageId);
}
