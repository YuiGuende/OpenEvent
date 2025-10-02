//package com.group02.openevent.service.impl;
//
//import com.group02.openevent.model.ticket.Ticket;
//import com.group02.openevent.model.ticket.TicketStatus;
//import com.group02.openevent.model.user.User;
//import com.group02.openevent.model.event.Event;
//import com.group02.openevent.repository.ITicketRepo;
//import com.group02.openevent.repository.IEventRepo;
//import com.group02.openevent.repository.IUserRepo;
//import com.group02.openevent.service.TicketService;
//import com.group02.openevent.service.GuestService;
//import com.group02.openevent.dto.ticket.CreateTicketRequest;
//import com.group02.openevent.dto.ticket.TicketResponse;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.time.LocalDateTime;
//import java.util.*;
//import java.util.stream.Collectors;
//
//@Service
//@Transactional
//public class TicketServiceImpl implements TicketService {
//
//    private final ITicketRepo ticketRepo;
//    private final IEventRepo eventRepo;
//    private final IUserRepo userRepo;
//    private final GuestService guestService;
//
//    public TicketServiceImpl(ITicketRepo ticketRepo, IEventRepo eventRepo, IUserRepo userRepo, GuestService guestService) {
//        this.ticketRepo = ticketRepo;
//        this.eventRepo = eventRepo;
//        this.userRepo = userRepo;
//        this.guestService = guestService;
//    }
//
//    @Override
//    public Ticket createTicket(CreateTicketRequest request, User user) {
//        // Validate event exists
//        Optional<Event> eventOpt = eventRepo.findById(request.getEventId());
//        if (eventOpt.isEmpty()) {
//            throw new IllegalArgumentException("Event not found");
//        }
//
//        Event event = eventOpt.get();
//
//        // Check if user already registered for this event
//        if (hasUserRegisteredEvent(user.getUserId(), request.getEventId())) {
//            throw new IllegalArgumentException("User has already registered for this event");
//        }
//
//        // Create ticket
//        Ticket ticket = new Ticket();
//        ticket.setUser(user);
//        ticket.setEvent(event);
//        ticket.setTicketCode(generateTicketCode());
//        ticket.setPrice(request.getPrice());
//        ticket.setStatus(TicketStatus.PENDING);
//        // PayOS yêu cầu description tối đa 25 ký tự
//        String description = "Event Registration";
//        ticket.setDescription(description);
//        ticket.setParticipantName(request.getParticipantName());
//        ticket.setParticipantEmail(request.getParticipantEmail());
//        ticket.setParticipantPhone(request.getParticipantPhone());
//        ticket.setParticipantOrganization(request.getParticipantOrganization());
//        ticket.setNotes(request.getNotes());
//        ticket.setTicketTypeName(request.getTicketTypeName());
//
//        return ticketRepo.save(ticket);
//    }
//
//    @Override
//    public Optional<Ticket> getTicketById(Long ticketId) {
//        return ticketRepo.findById(ticketId);
//    }
//
//    @Override
//    public Optional<Ticket> getTicketByCode(String ticketCode) {
//        return ticketRepo.findByTicketCode(ticketCode);
//    }
//
//    @Override
//    public List<Ticket> getTicketsByUser(User user) {
//        return ticketRepo.findByUser(user);
//    }
//
//    @Override
//    public List<Ticket> getTicketsByUserId(Long userId) {
//        return ticketRepo.findByUserId(userId);
//    }
//
//    @Override
//    public List<Ticket> getTicketsByUserIdAndStatus(Long userId, TicketStatus status) {
//        return ticketRepo.findByUserIdAndStatus(userId, status);
//    }
//
//    @Override
//    public void updateTicketStatus(Ticket ticket, TicketStatus status) {
//        ticket.setStatus(status);
//        ticketRepo.save(ticket);
//    }
//
//    @Override
//    public boolean cancelTicket(Ticket ticket) {
//        if (ticket.getStatus() == TicketStatus.PENDING) {
//            ticket.setStatus(TicketStatus.CANCELLED);
//            ticketRepo.save(ticket);
//            return true;
//        }
//        return false;
//    }
//
//    @Override
//    public String generateTicketCode() {
//        String timestamp = String.valueOf(System.currentTimeMillis());
//        String random = String.valueOf((int) (Math.random() * 1000));
//        return "TKT" + timestamp.substring(timestamp.length() - 8) + random;
//    }
//
//    @Override
//    public void updateExpiredTickets() {
//        // Tickets expire after 15 minutes if not paid
//        LocalDateTime expiredTime = LocalDateTime.now().minusMinutes(15);
//        List<Ticket> expiredTickets = ticketRepo.findExpiredPendingTickets(expiredTime);
//
//        for (Ticket ticket : expiredTickets) {
//            ticket.setStatus(TicketStatus.EXPIRED);
//            ticketRepo.save(ticket);
//        }
//    }
//
//    @Override
//    public TicketResponse getTicketStatistics(Long userId) {
//        List<Ticket> allTickets = getTicketsByUserId(userId);
//
//        // Group tickets by status
//        Map<String, Long> statistics = allTickets.stream()
//                .collect(Collectors.groupingBy(
//                        ticket -> ticket.getStatus().name(),
//                        Collectors.counting()
//                ));
//
//        // Add total count
//        statistics.put("TOTAL", (long) allTickets.size());
//
//        TicketResponse response = new TicketResponse();
//        response.setStatistics(statistics);
//
//        return response;
//    }
//
//    @Override
//    public boolean hasUserRegisteredEvent(Long userId, Long eventId) {
//        List<Ticket> userTickets = getTicketsByUserId(userId);
//        return userTickets.stream()
//                .anyMatch(ticket -> ticket.getEvent().getId().equals(eventId) &&
//                                 ticket.getStatus() == TicketStatus.PAID);
//    }
//
//    @Override
//    public void joinEventAfterPayment(Long userId, Long eventId) {
//        // Lấy user và event
//        Optional<User> userOpt = userRepo.findById(userId);
//        Optional<Event> eventOpt = eventRepo.findById(eventId);
//
//        if (userOpt.isEmpty() || eventOpt.isEmpty()) {
//            throw new IllegalArgumentException("User or Event not found");
//        }
//
//        User user = userOpt.get();
//        Event event = eventOpt.get();
//
//        // Kiểm tra user đã tham gia event chưa
//        if (!guestService.hasUserJoinedEvent(user, event)) {
//            // User tham gia event (trở thành guest)
//            guestService.joinEvent(user, event);
//        }
//    }
//}
