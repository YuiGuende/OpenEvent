package com.group02.openevent.repository;

import com.group02.openevent.model.event.EventImage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IEventImage extends JpaRepository<EventImage, Integer> {
}
