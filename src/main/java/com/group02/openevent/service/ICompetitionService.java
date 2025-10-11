package com.group02.openevent.service;

import com.group02.openevent.model.dto.event.CompetitionEventDetailDTO;
import java.util.List;

public interface ICompetitionService {
    CompetitionEventDetailDTO getCompetitionEventById(Long id);
    List<CompetitionEventDetailDTO> getAllCompetitionEvents();
}