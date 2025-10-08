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
            
        } catch (Exception e) {
            System.err.println("Error loading event: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("error", "Không thể tải dữ liệu sự kiện: " + e.getMessage());
        }
        return "music/musicHome"; // file musicHome.html
    }
}
