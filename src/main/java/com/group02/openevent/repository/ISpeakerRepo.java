package com.group02.openevent.repository;

import com.group02.openevent.model.event.Place;
import com.group02.openevent.model.event.Speaker;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ISpeakerRepo extends JpaRepository<Speaker, Long> {

    @Query("SELECT p FROM Speaker p JOIN p.events e WHERE e.id = :eventId")
    List<Speaker> findSpeakerByEventId(@Param("eventId") Long eventId);
}
