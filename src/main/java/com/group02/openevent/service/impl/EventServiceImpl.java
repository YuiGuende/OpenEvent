package com.group02.openevent.service.impl;

import com.group02.openevent.model.enums.EventType;
import com.group02.openevent.model.event.Event;
import com.group02.openevent.model.event.EventSchedule;
import com.group02.openevent.model.event.MusicEvent;
import com.group02.openevent.repository.IEventRepo;
import com.group02.openevent.repository.IMusicEventRepo;
import com.group02.openevent.service.EventService;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

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
//        List<EventSchedule> eventSchedules = musicEvent.getSchedules();
//        musicEvent.setSchedules(null);
//        musicEvent.setSpeakerLinks(null);
//        musicEvent.setPlaces(null);
////        MusicEvent musicEvent1 =
//        MusicEvent musicEvent1 = musicEventRepo.save(musicEvent);
//
////        if (eventSchedules != null) {
////            musicEvent1.setSchedules(eventSchedules);
////            return musicEventRepo.save(musicEvent1);
////        }
////        else {
////            return musicEvent1;
////        }
//
//
//        musicEvent1.setSchedules(eventSchedules);
        return eventRepo.save(musicEvent);


    }

    @Override
    public Optional<Event> getEventById(Integer id) {
        return eventRepo.findById(id);
    }
}
