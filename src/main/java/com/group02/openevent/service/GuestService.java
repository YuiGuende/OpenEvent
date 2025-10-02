package com.group02.openevent.service;

import com.group02.openevent.model.user.Guest;
import com.group02.openevent.model.user.User;
import com.group02.openevent.model.event.Event;

import java.util.List;
import java.util.Optional;

public interface GuestService {

    // User tham gia event (trở thành guest)
    Guest joinEvent(User user, Event event);
    
    // User rời khỏi event
    boolean leaveEvent(User user, Event event);
    
    // Host loại guest khỏi event
    boolean removeGuest(User user, Event event);
    
    // Lấy thông tin guest
    Optional<Guest> getGuestByUserAndEvent(User user, Event event);
    Optional<Guest> getGuestByUserIdAndEventId(Long userId, Long eventId);
    
    // Lấy danh sách guests của event
    List<Guest> getGuestsByEvent(Event event);
    List<Guest> getGuestsByEventId(Long eventId);
    
    // Lấy danh sách events mà user đã tham gia
    List<Guest> getEventsByUser(User user);
    List<Guest> getEventsByUserId(Long userId);
    
    // Lấy danh sách guests theo status
    List<Guest> getGuestsByStatus(Guest.GuestStatus status);
    List<Guest> getGuestsByEventAndStatus(Event event, Guest.GuestStatus status);
    
    // Kiểm tra user đã tham gia event chưa
    boolean hasUserJoinedEvent(User user, Event event);
    boolean hasUserJoinedEvent(Long userId, Long eventId);
    
    // Đếm số guests
    long countGuestsByEvent(Event event);
    long countGuestsByEventAndStatus(Event event, Guest.GuestStatus status);
    
    // Cập nhật status của guest
    void updateGuestStatus(Guest guest, Guest.GuestStatus status);
}
