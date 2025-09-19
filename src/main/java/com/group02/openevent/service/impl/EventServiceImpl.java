package com.group02.openevent.service.impl;

import com.group02.openevent.model.enums.EventType;
import com.group02.openevent.model.enums.EventStatus;
import com.group02.openevent.model.event.Event;
import com.group02.openevent.model.event.EventSchedule;
import com.group02.openevent.model.event.MusicEvent;
import com.group02.openevent.repository.IEventRepo;
import com.group02.openevent.repository.IMusicEventRepo;
import com.group02.openevent.service.EventService;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

@Service
public class EventServiceImpl implements EventService {
    @Autowired
    private IMusicEventRepo musicEventRepo;

    @Autowired
    private IEventRepo eventRepo;


    @Override
    public Event saveEvent(Event event) {
        // đảm bảo schedule biết event cha
        if (event.getSchedules() != null) {
            event.getSchedules().forEach(s -> s.setEvent(event));
            System.out.println("debug");
        }

        return eventRepo.save(event);
    }

    @Override
    public MusicEvent saveMusicEvent(MusicEvent musicEvent) {
        return eventRepo.save(musicEvent);
    }

    @Override
    public Optional<Event> getEventById(Integer id) {
        return eventRepo.findById(id);
    }

    @Override
    public List<Event> getEventsByType(Class<? extends Event> eventType) {
        return eventRepo.findByEventType(eventType);
    }

    @Override
    public Page<Event> listEvents(EventType eventType, EventStatus status, Pageable pageable) {
        if (eventType != null && status != null) {
            return eventRepo.findByEventTypeAndStatus(eventType, status, pageable);
        } else if (eventType != null) {
            return eventRepo.findByEventType(eventType, pageable);
        } else if (status != null) {
            return eventRepo.findByStatus(status, pageable);
        } else {
            return eventRepo.findAll(pageable);
        }
    }
}
