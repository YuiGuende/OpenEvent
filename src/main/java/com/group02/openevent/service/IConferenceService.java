package com.group02.openevent.service;

import com.group02.openevent.model.dto.event.ConferenceEventDetailDTO;
import com.group02.openevent.model.event.EventImage;

import java.util.List;

public interface IConferenceService {
    List<ConferenceEventDetailDTO> getAllConferenceEvents();
    ConferenceEventDetailDTO getConferenceEventById(Long id);
    List<EventImage> getEventImages(Long eventId);
}