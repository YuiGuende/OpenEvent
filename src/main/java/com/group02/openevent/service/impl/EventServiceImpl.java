package com.group02.openevent.service.impl;

import com.group02.openevent.mapper.EventMapper;
import com.group02.openevent.model.dto.request.EventCreationRequest;
import com.group02.openevent.model.dto.response.EventResponse;
import com.group02.openevent.model.enums.EventType;
import com.group02.openevent.model.event.Event;
import com.group02.openevent.model.event.EventSchedule;
import com.group02.openevent.model.event.MusicEvent;
import com.group02.openevent.repository.IEventRepo;
import com.group02.openevent.repository.IMusicEventRepo;
import com.group02.openevent.service.EventService;
import com.sun.jdi.request.EventRequest;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class EventServiceImpl implements EventService {

    IMusicEventRepo musicEventRepo;
    IEventRepo eventRepo;
    EventMapper eventMapper;

    @Override
    public EventResponse saveEvent(EventCreationRequest request) {
        // đảm bảo schedule biết event cha
        Event event = eventMapper.toEvent(request);

        if (event.getSchedules() != null) {
            event.getSchedules().forEach(s -> s.setEvent(event));
        }
        event.setSpeakers(request.getSpeakers());
        event.setPlaces(request.getPlaces());
        return eventMapper.toEventResponse(eventRepo.save(event));
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
    public Optional<Event> getEventById(Long id) {
        return eventRepo.findById(id);
    }
}
