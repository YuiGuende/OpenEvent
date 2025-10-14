package com.group02.openevent.repository;

import com.group02.openevent.model.event.EventSchedule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IEventScheduleRepo extends JpaRepository<EventSchedule,Long> {
    List<EventSchedule> findByEventId(Long eventId);
}
