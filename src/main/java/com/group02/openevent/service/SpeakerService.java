package com.group02.openevent.service;

import com.group02.openevent.model.event.Speaker;

import java.util.List;

public interface SpeakerService {
    public List<Speaker> findAllByEventId(Long id);
    public void deleteById(Long id);
    public Speaker update (Long id , Speaker speaker);
    Speaker create(Speaker request);
    Speaker addToEvent(Long speakerId, Long eventId);
}
