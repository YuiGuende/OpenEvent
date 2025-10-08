package com.group02.openevent.controller.event;

import com.group02.openevent.model.dto.ScheduleDTO;
import com.group02.openevent.model.dto.event.FestivalEventDetailDTO;
import com.group02.openevent.model.event.EventImage;
import com.group02.openevent.model.ticket.TicketType;
import com.group02.openevent.service.IFestivalService;
import com.group02.openevent.service.TicketTypeService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
public class FestivalController {
    private final IFestivalService festivalService;
    private final TicketTypeService ticketTypeService;

    public FestivalController(IFestivalService festivalService, TicketTypeService ticketTypeService) {
        this.festivalService = festivalService;
        this.ticketTypeService = ticketTypeService;
    }

    @GetMapping("/festival/test")
    @ResponseBody
    public String testFestival() {
        return "Festival Controller is working!";
    }
    
    @GetMapping("/festival/list")
    @ResponseBody
    public String listFestivals() {
        try {
            var festivals = festivalService.getAllFestivalEvents();
            return "Found " + festivals.size() + " festival events. IDs: " + 
                   festivals.stream().map(f -> f.getId().toString()).collect(Collectors.joining(", "));
        } catch (Exception e) {
            return "Error listing festivals: " + e.getMessage();
        }
    }
    
    @GetMapping("/festival/{id}/debug")
    @ResponseBody
    public String debugFestival(@PathVariable Long id) {
        try {
            FestivalEventDetailDTO event = festivalService.getFestivalEventById(id);
            if (event == null) {
                return "No festival event found for id: " + id;
            }
            
            StringBuilder debug = new StringBuilder();
            debug.append("Festival Event ID: ").append(event.getId()).append("\n");
            debug.append("Title: ").append(event.getTitle()).append("\n");
            debug.append("Event Images Count: ").append(event.getEventImages() != null ? event.getEventImages().size() : 0).append("\n");

            if (event.getEventImages() != null && !event.getEventImages().isEmpty()) {
                debug.append("Event Images:\n");
                int i = 1;
                for (EventImage image : event.getEventImages()) {
                    debug.append(" ").append(i++).append(": ").append(image).append("\n");
                }
            } else {
                debug.append("No event images found.\n");
            }


            return debug.toString();
        } catch (Exception e) {
            return "Error debugging festival: " + e.getMessage();
        }
    }

    @GetMapping("/festival/{id}")
    public String showFestivalEventDetail(@PathVariable Long id, Model model) {
        System.out.println("DEBUG: Getting FestivalEvent with id: " + id);
        
        try {
            FestivalEventDetailDTO event = festivalService.getFestivalEventById(id);
            if (event == null) {
                System.out.println("DEBUG: No festival event found for id " + id);
                model.addAttribute("errorMessage", "Festival event not found");
                return "error/404";
            }

            // Group schedules by day (with null check)
            Map<LocalDate, List<ScheduleDTO>> schedulesByDay = Map.of();
            List<Map.Entry<LocalDate, List<ScheduleDTO>>> scheduleEntries = List.of();
            
            if (event.getSchedules() != null && !event.getSchedules().isEmpty()) {
                schedulesByDay = event.getSchedules().stream()
                        .collect(Collectors.groupingBy(sc -> sc.getStartTime().toLocalDate()));

                // Convert to List for Thymeleaf iteration
                scheduleEntries = schedulesByDay.entrySet()
                        .stream()
                        .sorted(Map.Entry.comparingByKey()) // Sort by date
                        .collect(Collectors.toList());
            }
            
            List<TicketType> ticketTypes = ticketTypeService.getTicketTypesByEventId(id);

            model.addAttribute("schedulesByDay", schedulesByDay);
            model.addAttribute("scheduleEntries", scheduleEntries);
            model.addAttribute("tickets", ticketTypes);
            model.addAttribute("festivalEvent", event); // Template expects 'festivalEvent'
            model.addAttribute("eventImages", event.getEventImages() != null ? event.getEventImages() : List.of());

            return "festival/festivalHome";
        } catch (RuntimeException e) {
            System.out.println("DEBUG: Festival event not found for id " + id + ": " + e.getMessage());
            model.addAttribute("errorMessage", "Festival event not found with id: " + id);
            return "error/404";
        } catch (Exception e) {
            System.out.println("DEBUG: Error getting festival event: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("errorMessage", "An error occurred while loading the festival event");
            return "error/500";
        }
    }
}