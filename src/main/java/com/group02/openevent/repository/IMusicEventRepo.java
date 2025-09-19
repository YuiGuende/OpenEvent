package com.group02.openevent.repository;

import com.group02.openevent.model.event.MusicEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IMusicEventRepo extends JpaRepository<MusicEvent, Integer> {
}

