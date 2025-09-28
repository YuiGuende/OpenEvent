package com.group02.openevent.repository;


import com.group02.openevent.model.event.Event;
import com.group02.openevent.model.enums.EventType;
import com.group02.openevent.model.enums.EventStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import javax.swing.text.html.Option;
import java.util.List;
import java.util.Optional;

@Repository
public interface IEventRepo extends JpaRepository<Event, Long> {

    @Query("SELECT e FROM Event e WHERE TYPE(e) = :eventType")
    List<Event> findByEventType(@Param("eventType") Class<? extends Event> eventType);

    List<Event> findByStatus(String status);

    // Pageable listing
    Page<Event> findAll(Pageable pageable);
    Page<Event> findByEventType(EventType eventType, Pageable pageable);
    Page<Event> findByStatus(EventStatus status, Pageable pageable);
    Page<Event> findByEventTypeAndStatus(EventType eventType, EventStatus status, Pageable pageable);
}