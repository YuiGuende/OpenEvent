package com.group02.openevent.service;

import com.group02.openevent.model.dto.workshop.WorkshopEventDetailDTO;
import com.group02.openevent.model.event.EventImage;

import java.util.List;

public interface IWorkshopService {
    List<WorkshopEventDetailDTO> getAllWorkshopEvents();
    List<EventImage> getEventImages(Long eventId);
    WorkshopEventDetailDTO getWorkshopEventById(Long id);
}
