package com.group02.openevent.controller.ticket;


import com.group02.openevent.dto.ticket.TicketTypeDTO;
import com.group02.openevent.service.TicketTypeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ticket-types")
@RequiredArgsConstructor
public class TicketTypeController {

    private final TicketTypeService ticketTypeService;

    @GetMapping("/event/{eventId}")
    public ResponseEntity<List<TicketTypeDTO>> getTicketTypesByEvent(@PathVariable Long eventId) {
        try {
            System.out.println("Getting ticket types for event: " + eventId);
            List<TicketTypeDTO> ticketTypes = ticketTypeService.getTicketTypeDTOsByEventId(eventId);
            System.out.println("ticketTypes =369 " + ticketTypes.size());
            return ResponseEntity.ok(ticketTypes);
        } catch (Exception e) {
            System.err.println("Error getting ticket types: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).build();
        }
    }

    @GetMapping("/event/{eventId}/available")
    public ResponseEntity<List<TicketTypeDTO>> getAvailableTicketTypesByEvent(@PathVariable Long eventId) {
        List<TicketTypeDTO> ticketTypes = ticketTypeService.getAvailableTicketTypeDTOsByEventId(eventId);
        return ResponseEntity.ok(ticketTypes);
    }

    @GetMapping("/{ticketTypeId}")
    public ResponseEntity<TicketTypeDTO> getTicketTypeById(@PathVariable Long ticketTypeId) {
        return ticketTypeService.getTicketTypeDTOById(ticketTypeId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}