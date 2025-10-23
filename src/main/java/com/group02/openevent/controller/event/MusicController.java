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
import org.springframework.web.bind.annotation.RequestParam;

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
    public String getMusicEventDetail(@PathVariable("id") Long id, 
                                     @RequestParam(required = false) String error,
                                     @RequestParam(required = false) String success,
                                     @RequestParam(required = false) String message,
                                     Model model) {
        try {
            MusicEventDetailDTO event = musicService.getMusicEventById(id);
            List<EventImage> eventImages = musicService.getEventImages(id);

            List<TicketType> ticketTypes = ticketTypeService.getTicketTypesByEventId(id);

            model.addAttribute("event", event); // truyền 1 object duy nhất
            model.addAttribute("eventId", id); // thêm eventId cho feedback form
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
            
            // Pass error/success messages to the view
            if (error != null) {
                model.addAttribute("errorMessage", getErrorMessage(error));
            }
            if (success != null) {
                model.addAttribute("successMessage", getSuccessMessage(success));
            }
            if (message != null) {
                model.addAttribute("detailMessage", message);
            }
            
        } catch (Exception e) {
            System.err.println("Error loading event: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("error", "Không thể tải dữ liệu sự kiện: " + e.getMessage());
        }
        return "music/musicHome"; // file musicHome.html
    }
    
    private String getErrorMessage(String error) {
        return switch (error) {
            case "form_creation_failed" -> "Failed to create feedback form. Please try again.";
            case "no_form_found" -> "No feedback form found for this event.";
            case "submission_failed" -> "Failed to submit feedback. Please try again.";
            case "missing_event_id" -> "Invalid event ID.";
            default -> "An error occurred. Please try again.";
        };
    }
    
    private String getSuccessMessage(String success) {
        return switch (success) {
            case "form_created" -> "Feedback form created successfully!";
            case "feedback_submitted" -> "Thank you for your feedback!";
            default -> "Operation completed successfully.";
        };
    }
}
