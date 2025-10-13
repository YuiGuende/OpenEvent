package com.group02.openevent.service;

import com.group02.openevent.model.event.EventSchedule;

import java.util.List;

public interface EventScheduleService {
    List<EventSchedule> findByEventId(Long eventId);
    EventSchedule create(EventSchedule schedule);
    EventSchedule update(Long id, EventSchedule schedule);
    void deleteById(Long id);
}
