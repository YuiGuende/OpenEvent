package com.group02.openevent.repository;

import com.group02.openevent.model.event.Place;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
@Repository
public interface IPlaceRepo extends JpaRepository<Place,Long>
{
    @Query("SELECT p FROM Place p JOIN p.events e WHERE e.id = :eventId")
    List<Place> findPlacesByEventId(@Param("eventId") Long eventId);
    
    // Alternative query using native SQL
    @Query(value = "SELECT p.* FROM place p INNER JOIN event_place ep ON p.place_id = ep.place_id WHERE ep.event_id = :eventId", nativeQuery = true)
    List<Place> findPlacesByEventIdNative(@Param("eventId") Long eventId);
}
