package com.group02.openevent.repository;


import com.group02.openevent.model.event.Event;
import com.group02.openevent.model.enums.EventType;
import com.group02.openevent.model.enums.EventStatus;
import com.group02.openevent.model.event.Place;
import com.group02.openevent.model.event.Speaker;
import com.group02.openevent.model.user.Host;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import javax.swing.text.html.Option;
import java.time.LocalDate;
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
    @Query("SELECT e FROM Event e WHERE e.title = :title AND e.status = 'PUBLIC'")
    List<Event> findByTitleAndPublicStatus(@Param("title") String title);
    @Query("SELECT e FROM Event e WHERE e.host.id = :userId AND e.startsAt > :now ORDER BY e.startsAt ASC LIMIT 1")
    Optional<Event> findNextUpcomingEventByUserId(
            @Param("userId") Long userId,
            @Param("now") LocalDateTime now
    );
    List<Event> findByStatus(EventStatus status);
    // Pageable listing
    @Query("SELECT e FROM Event e JOIN e.places p WHERE p.id = :placeId")
    List<Event> findByPlaceId(@Param("placeId") Long placeId);
    Page<Event> findAll(Pageable pageable);
    Page<Event> findByEventType(EventType eventType, Pageable pageable);
    Page<Event> findByStatus(EventStatus status, Pageable pageable);
    Page<Event> findByEventTypeAndStatus(EventType eventType, EventStatus status, Pageable pageable);

    @Query("SELECT e FROM Event e WHERE e.poster = true")
    List<Event> findByPosterTrue();


    @Query("SELECT e FROM Event e WHERE e.status = :status ORDER BY e.createdAt DESC")
    List<Event> findRecommendedEvents(@Param("status") EventStatus status, Pageable pageable);
    List<Event> getEventByHostId(Long hostId);
    List<Event> findBySpeakersContains(Speaker speaker);

    Page<Event> findByDepartment_AccountIdAndEventTypeAndStatus(Long departmentId, EventType eventType, EventStatus status, Pageable pageable);

    Page<Event> findByDepartment_AccountIdAndEventType(Long departmentId, EventType eventType, Pageable pageable);

    Page<Event> findByDepartment_AccountId(Long departmentId, Pageable pageable);

    Page<Event> findByDepartment_AccountIdAndStatus(Long departmentId, EventStatus status, Pageable pageable);

    @Query("""
    SELECT e FROM Event e
    WHERE (:keyword IS NULL OR LOWER(e.title) LIKE LOWER(CONCAT('%', :keyword, '%')))
      AND (:type IS NULL OR e.eventType = :type)
      AND (:fromDateTime IS NULL OR e.startsAt >= :fromDateTime)
      AND (:toDateTime IS NULL OR e.startsAt <= :toDateTime)
""")
    List<Event> searchEvents(@Param("keyword") String keyword,
                             @Param("type") EventType type,
                             @Param("fromDateTime") LocalDateTime fromDateTime,
                             @Param("toDateTime") LocalDateTime toDateTime);






}