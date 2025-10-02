package com.group02.openevent.repository;

import com.group02.openevent.model.event.Event;
import com.group02.openevent.model.event.MusicEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
@Repository
public interface IMusicEventRepo extends JpaRepository<MusicEvent, Integer> {
    //có thể thêm nếu như cần
}

