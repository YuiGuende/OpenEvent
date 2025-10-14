package com.group02.openevent.repository;

import com.group02.openevent.model.event.CompetitionEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ICompetitionEventRepo extends JpaRepository<CompetitionEvent, Long> {
    //JPA có sẵn findAll(), findById(), save().
}
