package com.group02.openevent.controller.event;

import com.group02.openevent.model.dto.ScheduleDTO;
import com.group02.openevent.model.dto.competition.CompetitionEventDetailDTO;
import com.group02.openevent.service.ICompetitionService;
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

    public CompetitionController(ICompetitionService competitionService) {
        this.competitionService = competitionService;
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

        model.addAttribute("schedulesByDay", schedulesByDay);
        model.addAttribute("scheduleEntries", scheduleEntries);;

        model.addAttribute("event", event);
        model.addAttribute("eventImages", event.getImageUrls() != null ? event.getImageUrls() : List.of());

        return "competition/competitionHome"; // Đường dẫn tới templates/competition/competitionHome.html
    }
}
