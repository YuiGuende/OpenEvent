package com.group02.openevent.service;

import com.group02.openevent.model.dto.event.MusicEventDetailDTO;
import com.group02.openevent.model.event.EventImage;

import java.util.List;
public interface IMusicService {
    List<MusicEventDetailDTO> getAllMusicEvents();


    MusicEventDetailDTO getMusicEventById(Long id);

}
