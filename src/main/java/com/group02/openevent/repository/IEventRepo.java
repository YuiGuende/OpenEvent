package com.group02.openevent.repository;


import com.group02.openevent.model.event.Event;
import com.group02.openevent.model.event.EventType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IEventRepo extends JpaRepository<Event, Integer> {

    List<Event> findByEventType(EventType eventType);

    List<Event> findByStatus(String status);
}
