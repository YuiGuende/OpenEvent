package com.group02.openevent.repository;

import com.group02.openevent.model.user.Guest;
import com.group02.openevent.model.user.User;
import com.group02.openevent.model.event.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IGuestRepo extends JpaRepository<Guest, Long> {

    // Tìm guest theo user và event
    Optional<Guest> findByUserAndEvent(User user, Event event);
    
    // Tìm guest theo user_id và event_id
    @Query("SELECT g FROM Guest g WHERE g.user.userId = :userId AND g.event.id = :eventId")
    Optional<Guest> findByUserIdAndEventId(@Param("userId") Long userId, @Param("eventId") Long eventId);
    
    // Lấy danh sách guests của một event
    List<Guest> findByEvent(Event event);
    
    // Lấy danh sách guests của event theo event_id
    @Query("SELECT g FROM Guest g WHERE g.event.id = :eventId")
    List<Guest> findByEventId(@Param("eventId") Long eventId);
    
    // Lấy danh sách events mà user đã tham gia
    List<Guest> findByUser(User user);
    
    // Lấy danh sách events mà user đã tham gia theo user_id
    @Query("SELECT g FROM Guest g WHERE g.user.userId = :userId")
    List<Guest> findByUserId(@Param("userId") Long userId);
    
    // Lấy danh sách guests theo status
    List<Guest> findByStatus(Guest.GuestStatus status);
    
    // Lấy danh sách guests của event theo status
    List<Guest> findByEventAndStatus(Event event, Guest.GuestStatus status);
    
    // Kiểm tra user đã tham gia event chưa
    boolean existsByUserAndEvent(User user, Event event);
    
    // Đếm số guests của event
    long countByEvent(Event event);
    
    // Đếm số guests của event theo status
    long countByEventAndStatus(Event event, Guest.GuestStatus status);
}
