package com.group02.openevent.controller;

import com.group02.openevent.dto.ticket.TicketTypeRequest;
import com.group02.openevent.dto.ticket.TicketTypeResponse;
import com.group02.openevent.model.event.Event;
import com.group02.openevent.model.ticket.TicketType;
import com.group02.openevent.service.EventService;
import com.group02.openevent.service.TicketTypeService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/ticket-types")
@CrossOrigin(origins = "*")
public class TicketTypeController {

    @Autowired
    private TicketTypeService ticketTypeService;

    @Autowired
    private EventService eventService;

    @GetMapping
    public ResponseEntity<List<TicketTypeResponse>> getAllTicketTypes() {
        List<TicketType> ticketTypes = ticketTypeService.getAllTicketTypes();
        List<TicketTypeResponse> responses = ticketTypes.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/page")
    public ResponseEntity<Page<TicketTypeResponse>> getTicketTypesPage(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<TicketType> ticketTypePage = ticketTypeService.getTicketTypesPageable(pageable);
        Page<TicketTypeResponse> responsePage = ticketTypePage.map(this::convertToResponse);
        return ResponseEntity.ok(responsePage);
    }

    @GetMapping("/event/{eventId}")
    public ResponseEntity<List<TicketTypeResponse>> getTicketTypesByEvent(@PathVariable Long eventId) {
        List<TicketType> ticketTypes = ticketTypeService.getTicketTypesByEventId(eventId);
        List<TicketTypeResponse> responses = ticketTypes.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/event/{eventId}/all")
    public ResponseEntity<List<TicketTypeResponse>> getAllTicketTypesByEvent(@PathVariable Long eventId) {
        List<TicketType> ticketTypes = ticketTypeService.getTicketTypesByEventId(eventId);
        List<TicketTypeResponse> responses = ticketTypes.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TicketTypeResponse> getTicketType(@PathVariable Long id) {
        return ticketTypeService.getTicketTypeById(id)
                .map(ticketType -> ResponseEntity.ok(convertToResponse(ticketType)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<TicketTypeResponse> createTicketType(@Valid @RequestBody TicketTypeRequest request) {
        try {
            Event event = eventService.getEventById(request.getEventId())
                    .orElseThrow(() -> new IllegalArgumentException("Event not found"));

            TicketType ticketType = convertToEntity(request);
            ticketType.setEvent(event);

            TicketType created = ticketTypeService.createTicketType(ticketType);
            return ResponseEntity.status(HttpStatus.CREATED).body(convertToResponse(created));
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<TicketTypeResponse> updateTicketType(@PathVariable Long id,
                                                               @Valid @RequestBody TicketTypeRequest request) {
        try {
            TicketType ticketType = convertToEntity(request);
            TicketType updated = ticketTypeService.updateTicketType(id, ticketType);
            return ResponseEntity.ok(convertToResponse(updated));
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTicketType(@PathVariable Long id) {
        try {
            ticketTypeService.deleteTicketType(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{id}/availability")
    public ResponseEntity<Map<String, Object>> checkAvailability(@PathVariable Long id,
                                                                 @RequestParam Integer quantity) {
        boolean available = ticketTypeService.canPurchaseTickets(id, quantity);

        return ticketTypeService.getTicketTypeById(id)
                .map(ticketType -> {
                    Map<String, Object> response = Map.of(
                            "available", available,
                            "requestedQuantity", quantity,
                            "availableQuantity", ticketType.getAvailableQuantity(),
                            "isSalePeriodActive", ticketType.isSalePeriodActive()
                    );
                    return ResponseEntity.ok(response);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/event/{eventId}/statistics")
    public ResponseEntity<Map<String, Object>> getEventTicketStatistics(@PathVariable Long eventId) {
        Integer totalSold = ticketTypeService.getTotalSoldByEventId(eventId);
        Integer totalAvailable = ticketTypeService.getTotalAvailableByEventId(eventId);

        Map<String, Object> statistics = Map.of(
                "eventId", eventId,
                "totalSold", totalSold,
                "totalAvailable", totalAvailable,
                "totalCapacity", totalSold + totalAvailable
        );

        return ResponseEntity.ok(statistics);
    }

    private TicketTypeResponse convertToResponse(TicketType ticketType) {
        return new TicketTypeResponse(
                ticketType.getTicketTypeId(),
                ticketType.getEvent().getId(),
                ticketType.getEvent().getTitle(),
                ticketType.getName(),
                ticketType.getDescription(),
                ticketType.getPrice(),
                ticketType.getTotalQuantity(),
                ticketType.getSoldQuantity(),
                ticketType.getAvailableQuantity(),
                ticketType.getStartSaleDate(),
                ticketType.getEndSaleDate(),
                ticketType.isAvailable(),
                ticketType.isSalePeriodActive()
        );
    }

    private TicketType convertToEntity(TicketTypeRequest request) {
        // Validate sale period
        if (!request.isValidSalePeriod()) {
            throw new IllegalArgumentException("End sale date must be after start sale date");
        }

        TicketType ticketType = new TicketType();
        ticketType.setName(request.getName());
        ticketType.setDescription(request.getDescription());
        ticketType.setPrice(request.getPrice());
        ticketType.setTotalQuantity(request.getTotalQuantity());
        ticketType.setStartSaleDate(request.getStartSaleDate());
        ticketType.setEndSaleDate(request.getEndSaleDate());
        return ticketType;
    }

}