package com.group02.openevent.service;

import com.group02.openevent.dto.music.MusicEventDetailDTO;
import com.group02.openevent.model.event.EventImage;

import java.util.List;
public interface IMusicService {
    List<MusicEventDetailDTO> getAllMusicEvents();

    List<EventImage> getEventImages(Long eventId);
    MusicEventDetailDTO getMusicEventById(Long id);

}
