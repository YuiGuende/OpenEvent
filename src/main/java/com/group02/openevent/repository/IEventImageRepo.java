package com.group02.openevent.repository;

import com.group02.openevent.model.event.EventImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IEventImageRepo extends JpaRepository<EventImage, Long> {
    List<EventImage> findByEventId(Long eventId);
    List<EventImage> findByEventIdAndMainPoster(Long eventId, boolean mainPoster);
}
