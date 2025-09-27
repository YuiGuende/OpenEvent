package com.group02.openevent.controller;

import com.group02.openevent.model.ticket.Ticket;
import com.group02.openevent.service.TicketService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Optional;

@Controller
public class PaymentPageController {

    private final TicketService ticketService;

    public PaymentPageController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @GetMapping("/payment/success")
    public String paymentSuccess(@RequestParam(required = false) String orderCode,
                                 @RequestParam(required = false) Long ticketId,
                                 Model model) {
        
        if (ticketId != null) {
            Optional<Ticket> ticketOpt = ticketService.getTicketById(ticketId);
            if (ticketOpt.isPresent()) {
                Ticket ticket = ticketOpt.get();
                model.addAttribute("ticket", ticket);
                model.addAttribute("successMessage", "Payment successful! Your ticket has been created.");
            } else {
                model.addAttribute("errorMessage", "Ticket not found.");
            }
        } else if (orderCode != null) {
            model.addAttribute("successMessage", "Payment successful! Order: " + orderCode);
        } else {
            model.addAttribute("successMessage", "Payment successful!");
        }
        
        return "payment/success";
    }

    @GetMapping("/payment/cancel")
    public String paymentCancel(@RequestParam(required = false) String orderCode,
                               Model model) {
        if (orderCode != null) {
            model.addAttribute("errorMessage", "Payment cancelled. Order: " + orderCode);
        } else {
            model.addAttribute("errorMessage", "Payment cancelled.");
        }
        return "payment/cancel";
    }

    @GetMapping("/payment/mock-success")
    public String mockPaymentSuccess(@RequestParam(required = false) String orderCode,
                                   Model model) {
        if (orderCode != null) {
            model.addAttribute("orderCode", orderCode);
        }
        return "payment/mock-success";
    }
}
