package com.group02.openevent.service.impl;

import com.group02.openevent.model.enums.EventType;

import com.group02.openevent.model.enums.EventStatus;
import com.group02.openevent.model.event.Event;
import com.group02.openevent.model.event.EventSchedule;
import com.group02.openevent.model.event.MusicEvent;
import com.group02.openevent.repository.IEventRepo;
import com.group02.openevent.repository.IMusicEventRepo;
import com.group02.openevent.service.EventService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.group02.openevent.model.event.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class EventServiceImpl implements EventService {
    @Autowired
    private IMusicEventRepo musicEventRepo;

    @Autowired
    private IEventRepo eventRepo;

    @Override
    public MusicEvent saveMusicEvent(MusicEvent musicEvent) {
        if (musicEvent.getSchedules() != null) {
            musicEvent.getSchedules().forEach(s -> s.setEvent(musicEvent));
        }
        return eventRepo.save(musicEvent);
    }

    @Override
    public CompetitionEvent saveCompetitionEvent(CompetitionEvent competitionEvent) {
        if (competitionEvent.getSchedules() != null) {
            competitionEvent.getSchedules().forEach(s -> s.setEvent(competitionEvent));
        }
        return eventRepo.save(competitionEvent);
    }

    @Override
    public FestivalEvent saveFestivalEvent(FestivalEvent festivalEvent) {
        if (festivalEvent.getSchedules() != null) {
            festivalEvent.getSchedules().forEach(s -> s.setEvent(festivalEvent));
        }
        return eventRepo.save(festivalEvent);
    }

    @Override
    public WorkshopEvent saveWorkshopEvent(WorkshopEvent workshopEvent) {
        if (workshopEvent.getSchedules() != null) {
            workshopEvent.getSchedules().forEach(s -> s.setEvent(workshopEvent));
        }
        return eventRepo.save(workshopEvent);
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
    public Event saveEvent(Event event) {
        return eventRepo.save(event);
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
    @Override
    public List<Event> isTimeConflict(LocalDateTime start, LocalDateTime end, List<Place> places) {
        return eventRepo.findConflictedEvents(start, end, places);
    }

    @Override
    public boolean removeEvent(int id) {
        if (eventRepo.existsById(id)) {
            eventRepo.deleteById(id);
            return true; // ✅ xóa thành công
        } else {
            return false; // ✅ không tìm thấy
        }
    }

    @Override
    public boolean deleteByTitle(String title) {
        List<Event> events = eventRepo.findByTitle(title);
        if (events.isEmpty()) {
            return false; // ✅ không tìm thấy sự kiện
        }
        eventRepo.deleteAll(events);
        return true; // ✅ xóa thành công
    }

    @Override
    public List<Event> findByTitle(String title) {
        return eventRepo.findByTitle(title);
    }

    @Override
    public List<Event> getAllEvents() {
        return eventRepo.findAll();
    }

//    @Override
//    public List<Event> getEventByUserId(Integer userId) {
//        return eventRepo.getEventByUserId(userId);
//    }

    @Override
    public Optional<Event> getEventByEventId(Integer eventId) {
        return eventRepo.findById(eventId);
    }

    @Override
    public Optional<Event> getFirstEventByTitle(String title) {
        List<Event> events = eventRepo.findByTitle(title);
        if (events.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(events.get(0)); // trả về sự kiện đầu tiên
    }
//    @Override
//    public Optional<Event> getNextUpcomingEventByUserId(int userId) {
//        return eventRepo.findNextUpcomingEventByUserId(userId, LocalDateTime.now());
//    }
    @Override
    public List<Event> getEventsByPlace(int placeId) {
        return eventRepo.findByPlaceId(placeId);
    }
    
    @Override
    public List<Event> getEventsBetween(LocalDateTime start, LocalDateTime end, Integer userId) {
        // TODO: Implement proper filtering by date range and user
        // For now, return all events filtered by date range
        return eventRepo.findAll().stream()
                .filter(event -> event.getStartsAt().isAfter(start) || event.getStartsAt().isEqual(start))
                .filter(event -> event.getEndsAt().isBefore(end) || event.getEndsAt().isEqual(end))
                .collect(java.util.stream.Collectors.toList());
    }
    
    @Override
    public List<Event> getEventByUserId(Integer userId) {
        // TODO: Implement proper user-based filtering
        // For now, return all events
        return eventRepo.findAll();
    }
}
