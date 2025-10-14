package com.group02.openevent.repository;


import com.group02.openevent.model.user.Host;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IHostRepo extends JpaRepository<Host, Long> {
    Host getHostById(Long id);
}
