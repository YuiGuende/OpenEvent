package com.group02.openevent.repository;

import com.group02.openevent.model.event.WorkshopEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IWorkshopEventRepo extends JpaRepository<WorkshopEvent, Integer> {

}

