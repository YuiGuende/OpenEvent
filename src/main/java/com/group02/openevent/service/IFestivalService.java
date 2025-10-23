package com.group02.openevent.service;

import com.group02.openevent.dto.event.FestivalEventDetailDTO;
import java.util.List;

public interface IFestivalService {
    FestivalEventDetailDTO getFestivalEventById(Long id);
    List<FestivalEventDetailDTO> getAllFestivalEvents();
}