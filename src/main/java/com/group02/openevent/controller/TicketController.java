package com.group02.openevent.controller;

import com.group02.openevent.model.account.Account;
import com.group02.openevent.model.ticket.Ticket;
import com.group02.openevent.model.user.User;
import com.group02.openevent.repository.IAccountRepo;
import com.group02.openevent.repository.IUserRepo;
import com.group02.openevent.service.TicketService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tickets")
public class TicketController {

    private final TicketService ticketService;
    private final IUserRepo userRepo;
    private final IAccountRepo accountRepo;

    public TicketController(TicketService ticketService, IUserRepo userRepo, IAccountRepo accountRepo) {
        this.ticketService = ticketService;
        this.userRepo = userRepo;
        this.accountRepo = accountRepo;
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
}