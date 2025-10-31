package com.group02.openevent.repository;

import com.group02.openevent.model.form.EventForm;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IEventFormRepo extends JpaRepository<EventForm, Long> {
    
    @Query("SELECT f FROM EventForm f WHERE f.event.id = :eventId AND f.isActive = true")
    Optional<EventForm> findByEventIdAndActive(@Param("eventId") Long eventId);
    
    @Query("SELECT f FROM EventForm f WHERE f.event.id = :eventId")
    List<EventForm> findByEventId(@Param("eventId") Long eventId);
    
    @Query("SELECT f FROM EventForm f WHERE f.event.id = :eventId AND f.isActive = true")
    List<EventForm> findActiveByEventId(@Param("eventId") Long eventId);

    @Query("SELECT f FROM EventForm f WHERE f.event.id = :eventId AND f.formType = :formType AND f.isActive = true")
    Optional<EventForm> findActiveByEventIdAndType(@Param("eventId") Long eventId,
                                                   @Param("formType") EventForm.FormType formType);

    @Query("SELECT f FROM EventForm f WHERE f.event.id = :eventId AND f.formType = :formType")
    List<EventForm> findByEventIdAndType(@Param("eventId") Long eventId,
                                         @Param("formType") EventForm.FormType formType);
}
