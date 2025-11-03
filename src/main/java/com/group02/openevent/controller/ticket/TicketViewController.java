package com.group02.openevent.controller.ticket;

import com.group02.openevent.dto.ticket.TicketTypeDTO;
import com.group02.openevent.model.event.Event;
import com.group02.openevent.service.EventService;
import com.group02.openevent.service.TicketTypeService;
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

    @GetMapping("/ticket/{eventId}")
    public String viewTickets(@PathVariable Long eventId,
                              @RequestParam(name = "selectedTicketId", required = false) Long selectedTicketId,
                              Model model) {
        // Get event details
        Optional<Event> eventOpt = eventService.getEventById(eventId);
        if (eventOpt.isEmpty()) {
            return "error/404";
        }

        Event event = eventOpt.get();

        // Get tickets for this event
        List<TicketTypeDTO> tickets = ticketTypeService.getTicketTypeDTOsByEventId(eventId);

        model.addAttribute("event", event);
        model.addAttribute("tickets", tickets);
        model.addAttribute("selectedTicketId", selectedTicketId);
        return "event/view-ticket";
    }
}