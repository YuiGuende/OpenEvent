package com.group02.openevent.controller;

import com.group02.openevent.model.ticket.Ticket;
import com.group02.openevent.model.user.User;
import com.group02.openevent.repository.IUserRepo;
import com.group02.openevent.service.TicketService;
import com.group02.openevent.dto.ticket.CreateTicketRequest;
import com.group02.openevent.dto.ticket.TicketResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/tickets")
public class TicketController {

    private final TicketService ticketService;
    private final IUserRepo userRepo;

    public TicketController(TicketService ticketService, IUserRepo userRepo) {
        this.ticketService = ticketService;
        this.userRepo = userRepo;
    }

    /**
     * Lấy tất cả tickets của user hiện tại
     */
    @GetMapping("/my-tickets")
    public ResponseEntity<?> getMyTickets(HttpSession session) {
        Long accountId = (Long) session.getAttribute("ACCOUNT_ID");
        if (accountId == null) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "User not logged in"));
        }

        User user = userRepo.findByAccount_AccountId(accountId).orElse(null);
        if (user == null) {
            return ResponseEntity.status(404).body(Map.of("success", false, "message", "User not found"));
        }

        List<Ticket> tickets = ticketService.getTicketsByUser(user);
        return ResponseEntity.ok(Map.of("success", true, "tickets", tickets));
    }

    /**
     * Tạo ticket mới
     */
    @PostMapping("/create")
    public ResponseEntity<?> createTicket(@RequestBody CreateTicketRequest request, HttpSession session) {
        Long accountId = (Long) session.getAttribute("ACCOUNT_ID");
        if (accountId == null) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "User not logged in"));
        }

        User user = userRepo.findByAccount_AccountId(accountId).orElse(null);
        if (user == null) {
            return ResponseEntity.status(404).body(Map.of("success", false, "message", "User not found"));
        }

        try {
            Ticket ticket = ticketService.createTicket(request, user);
            return ResponseEntity.ok(Map.of("success", true, "ticket", ticket));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * Lấy ticket theo ID
     */
    @GetMapping("/{ticketId}")
    public ResponseEntity<?> getTicketById(@PathVariable Long ticketId) {
        Optional<Ticket> ticket = ticketService.getTicketById(ticketId);
        if (ticket.isPresent()) {
            return ResponseEntity.ok(Map.of("success", true, "ticket", ticket.get()));
        } else {
            return ResponseEntity.status(404).body(Map.of("success", false, "message", "Ticket not found"));
        }
    }

    /**
     * Hủy ticket
     */
    @PostMapping("/{ticketId}/cancel")
    public ResponseEntity<?> cancelTicket(@PathVariable Long ticketId, HttpSession session) {
        Long accountId = (Long) session.getAttribute("ACCOUNT_ID");
        if (accountId == null) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "User not logged in"));
        }

        Optional<Ticket> ticketOpt = ticketService.getTicketById(ticketId);
        if (ticketOpt.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("success", false, "message", "Ticket not found"));
        }

        Ticket ticket = ticketOpt.get();
        boolean cancelled = ticketService.cancelTicket(ticket);
        
        if (cancelled) {
            return ResponseEntity.ok(Map.of("success", true, "message", "Ticket cancelled successfully"));
        } else {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Cannot cancel ticket"));
        }
    }

    /**
     * Lấy thống kê tickets của user
     */
    @GetMapping("/statistics")
    public ResponseEntity<?> getTicketStatistics(HttpSession session) {
        Long accountId = (Long) session.getAttribute("ACCOUNT_ID");
        if (accountId == null) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "User not logged in"));
        }

        User user = userRepo.findByAccount_AccountId(accountId).orElse(null);
        if (user == null) {
            return ResponseEntity.status(404).body(Map.of("success", false, "message", "User not found"));
        }

        TicketResponse statistics = ticketService.getTicketStatistics(user.getUserId());
        return ResponseEntity.ok(Map.of("success", true, "statistics", statistics.getStatistics()));
    }
}