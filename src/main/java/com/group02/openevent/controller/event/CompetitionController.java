package com.group02.openevent.controller.event;

import com.group02.openevent.model.dto.ScheduleDTO;
import com.group02.openevent.model.dto.event.CompetitionEventDetailDTO;
import com.group02.openevent.model.ticket.TicketType;
import com.group02.openevent.service.ICompetitionService;
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
public class CompetitionController {

    private final ICompetitionService competitionService;
    private final TicketTypeService ticketTypeService;

    public CompetitionController(ICompetitionService competitionService, TicketTypeService ticketTypeService) {
        this.competitionService = competitionService;
        this.ticketTypeService = ticketTypeService;
    }

    @GetMapping("/competition/{id}")
    public String showCompetitionEventDetail(@PathVariable Long id, Model model) {
        System.out.println("DEBUG: Getting CompetitionEvent with id: " + id);

        CompetitionEventDetailDTO event = competitionService.getCompetitionEventById(id);
        if (event == null) {
            System.out.println("DEBUG: No competition event found for id " + id);
            return "error/404"; // Hoặc redirect tới trang lỗi
        }

        Map<LocalDate, List<ScheduleDTO>> schedulesByDay = event.getSchedules().stream()
                .collect(Collectors.groupingBy(sc -> sc.getStartTime().toLocalDate()));

        // Convert to List for Thymeleaf iteration
        List<Map.Entry<LocalDate, List<ScheduleDTO>>> scheduleEntries = schedulesByDay.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByKey()) // Sort by date
                .collect(Collectors.toList());
        List<TicketType> ticketTypes = ticketTypeService.getTicketTypesByEventId(id);

        model.addAttribute("schedulesByDay", schedulesByDay);
        model.addAttribute("scheduleEntries", scheduleEntries);
        model.addAttribute("tickets", ticketTypes);

        model.addAttribute("event", event);
        model.addAttribute("eventImages", event.getImageUrls() != null ? event.getImageUrls() : List.of());

        return "competition/competitionHome"; // Đường dẫn tới templates/competition/competitionHome.html
    }
}
