package com.group02.openevent.repository;

import com.group02.openevent.model.event.ConferenceEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
@Repository
public interface IConferenceEventRepo extends JpaRepository<ConferenceEvent, Long> {
}
