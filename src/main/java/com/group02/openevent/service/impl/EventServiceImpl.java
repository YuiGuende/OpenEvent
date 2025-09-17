package com.group02.openevent.service.impl;

import com.group02.openevent.model.event.Event;
import com.group02.openevent.repository.IEventRepo;
import com.group02.openevent.service.EventService;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Optional;

@Service
public class EventServiceImpl implements EventService {

    private final IEventRepo eventRepo;

    @Autowired
    public EventServiceImpl(IEventRepo eventRepo) {
        this.eventRepo = eventRepo;
    }

    @Override
    public Event saveEvent(Event event) {
        // đảm bảo schedule biết event cha
        if (event.getSchedules() != null) {
            event.getSchedules().forEach(s -> s.setEvent(event));
        }
        return eventRepo.save(event);
    }

    @Override
    public Optional<Event> getEventById(Integer id) {
        return eventRepo.findById(id);
    }
}
