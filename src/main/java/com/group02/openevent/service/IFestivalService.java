package com.group02.openevent.service;

import com.group02.openevent.model.dto.event.FestivalEventDetailDTO;
import com.group02.openevent.model.event.EventImage;

import java.util.List;

public interface IFestivalService {
    List<FestivalEventDetailDTO> getAllFestivalEvents();
    FestivalEventDetailDTO getFestivalEventById(Long id);
    List<EventImage> getEventImages(Long eventId);
}
