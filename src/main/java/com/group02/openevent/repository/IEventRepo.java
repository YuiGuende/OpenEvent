package com.group02.openevent.repository;


import com.group02.openevent.model.event.Event;
import com.group02.openevent.model.enums.EventType;
import com.group02.openevent.model.enums.EventStatus;
import com.group02.openevent.model.event.Place;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface IEventRepo extends JpaRepository<Event, Long> {

    @Query("SELECT e FROM Event e WHERE TYPE(e) = :eventType")
    List<Event> findByEventType(@Param("eventType") Class<? extends Event> eventType);
    @Query("""
        SELECT DISTINCT e FROM Event e
        JOIN e.places p
        WHERE p IN :places
          AND (e.startsAt < :end AND e.endsAt > :start)
    """)
    List<Event> findConflictedEvents(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end, @Param("places") List<Place> places);
    List<Event> findByStatus(String status);
    boolean removeEventById(int id);
    boolean deleteEventByTitle(String title);
    List<Event> findByTitle(String title);
    List<Event> findByStatus(EventStatus status);
    // Pageable listing
    @Query("SELECT e FROM Event e JOIN e.places p WHERE p.id = :placeId")
    List<Event> findByPlaceId(@Param("placeId") int placeId);
    Page<Event> findAll(Pageable pageable);
    Page<Event> findByEventType(EventType eventType, Pageable pageable);
    Page<Event> findByStatus(EventStatus status, Pageable pageable);
    Page<Event> findByEventTypeAndStatus(EventType eventType, EventStatus status, Pageable pageable);
//    List<Event> getEventByUserId(Integer userId);
//    @Query("SELECT e FROM Event e WHERE e.user.id = :userId AND e.startsAt > :now ORDER BY e.startsAt ASC")
//    Optional<Event> findNextUpcomingEventByUserId(@Param("userId") int userId, @Param("now") LocalDateTime now);
//    @Query("SELECT e FROM Event e " +
//            "WHERE e.startsAt >= :start " +
//            "AND e.endsAt <= :end " +
//            "AND e.createdBy.id = :userId")
//    List<Event> findEventsBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end, @Param("userId") int userId);

    @Query("SELECT e FROM Event e WHERE e.poster = true")
    List<Event> findByPosterTrue();

    @Query("SELECT e FROM Event e WHERE e.status = :status ORDER BY e.createdAt DESC")
    List<Event> findRecommendedEvents(@Param("status") EventStatus status, Pageable pageable);
}