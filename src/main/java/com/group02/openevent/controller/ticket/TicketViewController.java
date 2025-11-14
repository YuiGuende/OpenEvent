package com.group02.openevent.controller.ticket;

import com.group02.openevent.dto.ticket.TicketTypeDTO;
import com.group02.openevent.model.event.Event;
import com.group02.openevent.model.user.Customer;
import com.group02.openevent.service.EventService;
import com.group02.openevent.service.OrderService;
import com.group02.openevent.service.TicketTypeService;
import com.group02.openevent.service.UserService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Optional;

@Controller
@RequiredArgsConstructor
public class TicketViewController {

    private final EventService eventService;
    private final TicketTypeService ticketTypeService;
    private final OrderService orderService;
    private final UserService userService;

    @GetMapping("/ticket/{eventId}")
    public String viewTickets(@PathVariable Long eventId,
                              @RequestParam(name = "selectedTicketId", required = false) Long selectedTicketId,
                              Model model,
                              HttpSession session) {
        // Get event details
        Optional<Event> eventOpt = eventService.getEventById(eventId);
        if (eventOpt.isEmpty()) {
            return "error/404";
        }

        Event event = eventOpt.get();

        // Check if user has already purchased a ticket for this event
        boolean hasPurchasedTicket = false;
        try {
            Customer customer = userService.getCurrentUser(session).getCustomer();
            if (customer != null) {
                hasPurchasedTicket = orderService.hasCustomerRegisteredForEvent(customer.getCustomerId(), eventId);
            }
        } catch (Exception e) {
            // User not logged in or not a customer - allow access
            hasPurchasedTicket = false;
        }

        // If user has already purchased ticket, redirect to orders page
        if (hasPurchasedTicket) {
            return "redirect:/orders?message=already_purchased";
        }

        // Get tickets for this event
        List<TicketTypeDTO> tickets = ticketTypeService.getTicketTypeDTOsByEventId(eventId);

        model.addAttribute("event", event);
        model.addAttribute("tickets", tickets);
        model.addAttribute("selectedTicketId", selectedTicketId);
        model.addAttribute("hasPurchasedTicket", false);
        return "event/view-ticket";
    }
}