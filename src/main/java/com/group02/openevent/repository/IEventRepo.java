package com.group02.openevent.repository;


import com.group02.openevent.model.event.Event;
import com.group02.openevent.model.enums.EventType;
import com.group02.openevent.model.enums.EventStatus;
import com.group02.openevent.model.event.Place;
import com.group02.openevent.model.event.Speaker;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface IEventRepo extends JpaRepository<Event, Long> {

    @EntityGraph(attributePaths = {"host", "organization", "department"})
    @Query("SELECT e FROM Event e WHERE TYPE(e) = :eventType")
    List<Event> findByEventType(@Param("eventType") Class<? extends Event> eventType);
    @EntityGraph(attributePaths = {"host", "organization", "department"})
    @Query("""
        SELECT DISTINCT e FROM Event e
        JOIN e.places p
        WHERE p IN :places
          AND (e.startsAt < :end AND e.endsAt > :start)
    """)
    List<Event> findConflictedEvents(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end, @Param("places") List<Place> places);
    @EntityGraph(attributePaths = {"host", "organization", "department"})
    List<Event> findByStatus(String status);
    boolean removeEventById(int id);
    boolean deleteEventByTitle(String title);
    @EntityGraph(attributePaths = {"host", "organization", "department"})
    List<Event> findByTitle(String title);
    @EntityGraph(attributePaths = {"host", "organization", "department"})
    @Query("SELECT e FROM Event e WHERE e.title = :title AND e.status = 'PUBLIC'")
    List<Event> findByTitleAndPublicStatus(@Param("title") String title);
    @Query("SELECT e FROM Event e WHERE e.host.id = :userId AND e.startsAt > :now ORDER BY e.startsAt ASC LIMIT 1")
    Optional<Event> findNextUpcomingEventByUserId(
            @Param("userId") Long userId,
            @Param("now") LocalDateTime now
    );
    @EntityGraph(attributePaths = {"host", "organization", "department"})
    List<Event> findByStatus(EventStatus status);
    // Pageable listing
    @EntityGraph(attributePaths = {"host", "organization", "department"})
    @Query("SELECT e FROM Event e JOIN e.places p WHERE p.id = :placeId")
    List<Event> findByPlaceId(@Param("placeId") Long placeId);
    @EntityGraph(attributePaths = {"host", "organization", "department"})
    Page<Event> findAll(Pageable pageable);
    @EntityGraph(attributePaths = {"host", "organization", "department"})
    Page<Event> findByEventType(EventType eventType, Pageable pageable);
    @EntityGraph(attributePaths = {"host", "organization", "department"})
    Page<Event> findByStatus(EventStatus status, Pageable pageable);
    @EntityGraph(attributePaths = {"host", "organization", "department"})
    Page<Event> findByEventTypeAndStatus(EventType eventType, EventStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"host", "organization", "department"})
    @Query("SELECT e FROM Event e WHERE e.poster = true")
    List<Event> findByPosterTrue();


    @EntityGraph(attributePaths = {"host", "organization", "department"})
    @Query("SELECT e FROM Event e WHERE e.status = :status ORDER BY e.createdAt DESC")
    List<Event> findRecommendedEvents(@Param("status") EventStatus status, Pageable pageable);
    @EntityGraph(attributePaths = {"host", "organization", "department"})
    List<Event> getEventByHostId(Long hostId);
    @EntityGraph(attributePaths = {"host", "organization", "department"})
    List<Event> findBySpeakersContains(Speaker speaker);

    @EntityGraph(attributePaths = {"host", "organization", "department"})
    @Query("SELECT e FROM Event e WHERE e.department.user.account.accountId = :departmentId AND e.eventType = :eventType AND e.status = :status")
    Page<Event> findByDepartment_AccountIdAndEventTypeAndStatus(@Param("departmentId") Long departmentId, @Param("eventType") EventType eventType, @Param("status") EventStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"host", "organization", "department"})
    @Query("SELECT e FROM Event e WHERE e.department.user.account.accountId = :departmentId AND e.eventType = :eventType")
    Page<Event> findByDepartment_AccountIdAndEventType(@Param("departmentId") Long departmentId, @Param("eventType") EventType eventType, Pageable pageable);

    @EntityGraph(attributePaths = {"host", "organization", "department"})
    @Query("SELECT e FROM Event e WHERE e.department.user.account.accountId = :departmentId")
    Page<Event> findByDepartment_AccountId(@Param("departmentId") Long departmentId, Pageable pageable);

    @EntityGraph(attributePaths = {"host", "organization", "department"})
    @Query("SELECT e FROM Event e WHERE e.department.user.account.accountId = :departmentId AND e.status = :status")
    Page<Event> findByDepartment_AccountIdAndStatus(@Param("departmentId") Long departmentId, @Param("status") EventStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"host", "organization", "department"})
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

    /**
     * Find event with host, customer, and account eagerly fetched for authorization checks
     */
    @Query("""
        SELECT e FROM Event e
        LEFT JOIN FETCH e.host h
        LEFT JOIN FETCH h.user u
        WHERE e.id = :eventId
    """)
    Optional<Event> findByIdWithHostAccount(@Param("eventId") Long eventId);

    /**
     * Find events that should be updated to ONGOING status:
     * - startsAt <= now (event has started or starts today)
     * - status is PUBLIC or DRAFT (not already ONGOING, FINISH, or CANCEL)
     * - endsAt > now (event hasn't finished yet)
     */
    @EntityGraph(attributePaths = {"host", "organization", "department"})
    @Query("""
        SELECT e FROM Event e
        WHERE e.startsAt <= :now
          AND (e.status = 'PUBLIC')
          AND e.endsAt > :now
    """)
    List<Event> findEventsToUpdateToOngoing(@Param("now") LocalDateTime now);

    /**
     * Filter events by host ID with search, status, and time filters
     */







}