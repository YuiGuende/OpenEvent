package com.group02.openevent.service;

import com.group02.openevent.model.dto.music.MusicEventDetailDTO;
import com.group02.openevent.model.event.EventImage;
import org.springframework.stereotype.Service;

import java.util.List;
public interface IMusicService {
    List<MusicEventDetailDTO> getAllMusicEvents();

    List<EventImage> getEventImages(Integer eventId);
    MusicEventDetailDTO getMusicEventById(Integer id);

}
