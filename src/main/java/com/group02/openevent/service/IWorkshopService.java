package com.group02.openevent.service;

import com.group02.openevent.dto.event.WorkshopEventDetailDTO;
import java.util.List;

public interface IWorkshopService {
    WorkshopEventDetailDTO getWorkshopEventById(Long id);
    List<WorkshopEventDetailDTO> getAllWorkshopEvents();
}