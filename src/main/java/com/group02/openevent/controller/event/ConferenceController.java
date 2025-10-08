package com.group02.openevent.controller.event;

import com.group02.openevent.model.dto.ScheduleDTO;
import com.group02.openevent.model.dto.event.ConferenceEventDetailDTO;
import com.group02.openevent.model.ticket.TicketType;
import com.group02.openevent.service.IConferenceService;
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
public class ConferenceController {

    private final IConferenceService conferenceService;
    private final TicketTypeService ticketTypeService;

    public ConferenceController(IConferenceService conferenceService, TicketTypeService ticketTypeService) {
        this.conferenceService = conferenceService;
        this.ticketTypeService = ticketTypeService;
    }

    @GetMapping("/conference/{id}")
    public String getConferenceEventDetail(@PathVariable("id") Long id, Model model) {
        try {
            ConferenceEventDetailDTO event = conferenceService.getConferenceEventById(id);
            List<TicketType> ticketTypes = ticketTypeService.getTicketTypesByEventId(id);

            model.addAttribute("event", event);
            model.addAttribute("tickets", ticketTypes);

            // Xử lý lịch trình giống như các controller khác
            if (event.getSchedules() != null) {
                List<Map.Entry<LocalDate, List<ScheduleDTO>>> scheduleEntries = event.getSchedules().stream()
                        .collect(Collectors.groupingBy(sc -> sc.getStartTime().toLocalDate()))
                        .entrySet().stream()
                        .sorted(Map.Entry.comparingByKey())
                        .collect(Collectors.toList());
                model.addAttribute("scheduleEntries", scheduleEntries);
            }
        } catch (Exception e) {
            System.err.println("Error loading conference event: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("error", "Không thể tải dữ liệu sự kiện hội thảo: " + e.getMessage());
        }
        return "conference/conferenceHome"; // Trả về file conferenceHome.html
    }
}