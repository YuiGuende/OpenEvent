package com.group02.openevent.repository;

import com.group02.openevent.model.event.FestivalEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IFestivalEventRepo extends JpaRepository<FestivalEvent, Long> {

}