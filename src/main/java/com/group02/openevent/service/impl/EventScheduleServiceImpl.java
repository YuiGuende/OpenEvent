package com.group02.openevent.service.impl;

import com.group02.openevent.model.event.Event;
import com.group02.openevent.model.event.EventSchedule;
import com.group02.openevent.repository.IEventRepo;
import com.group02.openevent.repository.IEventScheduleRepo;
import com.group02.openevent.service.EventScheduleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class EventScheduleServiceImpl implements EventScheduleService {
    
    @Autowired
    private IEventScheduleRepo scheduleRepo;
    
    @Autowired
    private IEventRepo eventRepo;

    @Override
    public List<EventSchedule> findByEventId(Long eventId) {
        return scheduleRepo.findByEventId(eventId);
    }

    @Override
    @Transactional
    public EventSchedule create(EventSchedule schedule) {
        // Load the event from database to ensure it exists
        Event event = eventRepo.findById(schedule.getEvent().getId())
                .orElseThrow(() -> new RuntimeException("Event not found with id: " + schedule.getEvent().getId()));
        
        // Set the loaded event
        schedule.setEvent(event);
        
        return scheduleRepo.save(schedule);
    }

    @Override
    @Transactional
    public EventSchedule update(Long id, EventSchedule schedule) {
        EventSchedule existingSchedule = scheduleRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Schedule not found with id: " + id));
        
        existingSchedule.setActivity(schedule.getActivity());
        existingSchedule.setStartTime(schedule.getStartTime());
        existingSchedule.setEndTime(schedule.getEndTime());
        existingSchedule.setDescription(schedule.getDescription());
        
        return scheduleRepo.save(existingSchedule);
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        scheduleRepo.deleteById(id);
    }
}
