package com.group02.openevent.service;

import com.group02.openevent.model.dto.competition.CompetitionEventDetailDTO;
import com.group02.openevent.model.event.CompetitionEvent;

import java.util.List;

public interface ICompetitionService {
    //take all competition event
    List<CompetitionEventDetailDTO> getAllCompetitionEvents();
    CompetitionEventDetailDTO getCompetitionEventById(Long id);
}
