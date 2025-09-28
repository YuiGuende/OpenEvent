package com.group02.openevent.service.impl;

import com.group02.openevent.model.user.Guest;
import com.group02.openevent.model.user.User;
import com.group02.openevent.model.event.Event;
import com.group02.openevent.repository.IGuestRepo;
import com.group02.openevent.service.GuestService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class GuestServiceImpl implements GuestService {

    private final IGuestRepo guestRepo;

    public GuestServiceImpl(IGuestRepo guestRepo) {
        this.guestRepo = guestRepo;
    }

    @Override
    public Guest joinEvent(User user, Event event) {
        // Kiểm tra user đã tham gia event chưa
        if (hasUserJoinedEvent(user, event)) {
            throw new IllegalArgumentException("User has already joined this event");
        }

        Guest guest = new Guest();
        guest.setUser(user);
        guest.setEvent(event);
        guest.setStatus(Guest.GuestStatus.ACTIVE);
        guest.setJoinedAt(java.time.LocalDateTime.now());

        return guestRepo.save(guest);
    }

    @Override
    public boolean leaveEvent(User user, Event event) {
        Optional<Guest> guestOpt = guestRepo.findByUserAndEvent(user, event);
        if (guestOpt.isPresent()) {
            Guest guest = guestOpt.get();
            guest.setStatus(Guest.GuestStatus.LEFT);
            guestRepo.save(guest);
            return true;
        }
        return false;
    }

    @Override
    public boolean removeGuest(User user, Event event) {
        Optional<Guest> guestOpt = guestRepo.findByUserAndEvent(user, event);
        if (guestOpt.isPresent()) {
            Guest guest = guestOpt.get();
            guest.setStatus(Guest.GuestStatus.REMOVED);
            guestRepo.save(guest);
            return true;
        }
        return false;
    }

    @Override
    public Optional<Guest> getGuestByUserAndEvent(User user, Event event) {
        return guestRepo.findByUserAndEvent(user, event);
    }

    @Override
    public Optional<Guest> getGuestByUserIdAndEventId(Long userId, Long eventId) {
        return guestRepo.findByUserIdAndEventId(userId, eventId);
    }

    @Override
    public List<Guest> getGuestsByEvent(Event event) {
        return guestRepo.findByEvent(event);
    }

    @Override
    public List<Guest> getGuestsByEventId(Long eventId) {
        // Cần tạo method findByEventId trong repository
        return guestRepo.findByEventId(eventId);
    }

    @Override
    public List<Guest> getEventsByUser(User user) {
        return guestRepo.findByUser(user);
    }

    @Override
    public List<Guest> getEventsByUserId(Long userId) {
        // Cần tạo method findByUserId trong repository
        return guestRepo.findByUserId(userId);
    }

    @Override
    public List<Guest> getGuestsByStatus(Guest.GuestStatus status) {
        return guestRepo.findByStatus(status);
    }

    @Override
    public List<Guest> getGuestsByEventAndStatus(Event event, Guest.GuestStatus status) {
        return guestRepo.findByEventAndStatus(event, status);
    }

    @Override
    public boolean hasUserJoinedEvent(User user, Event event) {
        return guestRepo.existsByUserAndEvent(user, event);
    }

    @Override
    public boolean hasUserJoinedEvent(Long userId, Long eventId) {
        return guestRepo.findByUserIdAndEventId(userId, eventId).isPresent();
    }

    @Override
    public long countGuestsByEvent(Event event) {
        return guestRepo.countByEvent(event);
    }

    @Override
    public long countGuestsByEventAndStatus(Event event, Guest.GuestStatus status) {
        return guestRepo.countByEventAndStatus(event, status);
    }

    @Override
    public void updateGuestStatus(Guest guest, Guest.GuestStatus status) {
        guest.setStatus(status);
        guestRepo.save(guest);
    }
}
