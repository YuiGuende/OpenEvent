package com.group02.openevent.controller.event;
import com.group02.openevent.model.dto.ScheduleDTO;
import com.group02.openevent.model.dto.competition.CompetitionEventDetailDTO;
import com.group02.openevent.service.ICompetitionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.ui.Model;

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
    public String showCompetitionEventDetail(@PathVariable("id") Long id, Model model) {
        CompetitionEventDetailDTO event = competitionService.getCompetitionEventById(id);

        // Nhóm lịch theo ngày (có thể thêm sau)
        Map<LocalDate, List<ScheduleDTO>> scheduleEntries = event.getSchedules().stream()
                .collect(Collectors.groupingBy(sc -> sc.getStartTime().toLocalDate()));
        model.addAttribute("scheduleEntries", scheduleEntries.entrySet());

        model.addAttribute("event", event);
        model.addAttribute("eventImages", event.getImageUrls());
        return "competition/competitionHome"; // <-- đường dẫn đến file .html
    }



}