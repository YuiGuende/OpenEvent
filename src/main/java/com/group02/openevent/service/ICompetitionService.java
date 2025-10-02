package com.group02.openevent.service;

import com.group02.openevent.model.dto.competition.CompetitionEventDetailDTO;
import com.group02.openevent.model.event.EventImage;

import java.util.List;

public interface ICompetitionService {
    List<CompetitionEventDetailDTO> getAllCompetitionEvents();
    List<EventImage> getEventImages(Integer eventId);
    CompetitionEventDetailDTO getCompetitionEventById(Integer id);
}
