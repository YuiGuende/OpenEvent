package com.group02.openevent.repository;

import com.group02.openevent.model.event.FestivalEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IFestivalEventRepo extends JpaRepository<FestivalEvent, Long> {
}

