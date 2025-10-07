package com.group02.openevent.controller.event;

import com.group02.openevent.dto.ScheduleDTO;
import com.group02.openevent.dto.music.MusicEventDetailDTO;
import com.group02.openevent.model.event.EventImage;
import com.group02.openevent.model.ticket.TicketType;
import com.group02.openevent.service.IMusicService;
import com.group02.openevent.service.TicketTypeService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
public class MusicController {

    private final IMusicService musicService;
    private final TicketTypeService ticketTypeService;

    public MusicController(IMusicService musicService, TicketTypeService ticketTypeService) {
        this.musicService = musicService;
        this.ticketTypeService = ticketTypeService;
    }

    @GetMapping("/music/{id}")
    public String getMusicEventDetail(@PathVariable("id") Long id, Model model) {
        try {
            MusicEventDetailDTO event = musicService.getMusicEventById(id);
            List<EventImage> eventImages = musicService.getEventImages(id);

            List<TicketType> ticketTypes = ticketTypeService.getTicketTypesByEventId(id);

            model.addAttribute("event", event); // truyền 1 object duy nhất
            model.addAttribute("eventImages", eventImages);
            model.addAttribute("tickets", ticketTypes);

            // Group schedules by day
            Map<LocalDate, List<ScheduleDTO>> schedulesByDay = event.getSchedules().stream()
                    .collect(Collectors.groupingBy(sc -> sc.getStartTime().toLocalDate()));
            
            // Convert to List for Thymeleaf iteration
            List<Map.Entry<LocalDate, List<ScheduleDTO>>> scheduleEntries = schedulesByDay.entrySet()
                    .stream()
                    .sorted(Map.Entry.comparingByKey()) // Sort by date
                    .collect(Collectors.toList());

            model.addAttribute("schedulesByDay", schedulesByDay);
            model.addAttribute("scheduleEntries", scheduleEntries);
            
            // Debug info
            System.out.println("=== EVENT DEBUG INFO ===");
            System.out.println("Event loaded: " + (event != null ? event.getTitle() : "NULL"));
            System.out.println("Event capacity: " + (event != null ? event.getCapacity() : "NULL"));
            System.out.println("Event places: " + (event != null && event.getPlaces() != null ? event.getPlaces().size() : 0));
            System.out.println("Event benefits: " + (event != null ? event.getBenefits() : "NULL"));
            System.out.println("Event startsAt: " + (event != null ? event.getStartsAt() : "NULL"));
            System.out.println("Event endsAt: " + (event != null ? event.getEndsAt() : "NULL"));
            System.out.println("Event images count: " + (eventImages != null ? eventImages.size() : 0));
            System.out.println("Ticket types count: " + (ticketTypes != null ? ticketTypes.size() : 0));
            if (ticketTypes != null && !ticketTypes.isEmpty()) {
                System.out.println("First ticket: " + ticketTypes.get(0).getName() + " - " + ticketTypes.get(0).getPrice());
            }
            System.out.println("========================");
            
        } catch (Exception e) {
            System.err.println("Error loading event: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("error", "Không thể tải dữ liệu sự kiện: " + e.getMessage());
        }
        return "music/musicHome"; // file musicHome.html
    }
}
