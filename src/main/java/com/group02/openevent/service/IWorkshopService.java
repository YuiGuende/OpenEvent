package com.group02.openevent.service;

import com.group02.openevent.model.dto.event.WorkshopEventDetailDTO;
import com.group02.openevent.model.event.EventImage;

import java.util.List;

public interface IWorkshopService {
    List<WorkshopEventDetailDTO> getAllWorkshopEvents();
    WorkshopEventDetailDTO getWorkshopEventById(Long id);
    List<EventImage> getEventImages(Long eventId);
}