package com.group02.openevent.controller.event;

import com.group02.openevent.model.dto.competition.CompetitionEventDetailDTO;
import com.group02.openevent.model.event.EventImage;
import com.group02.openevent.service.ICompetitionService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@Controller
public class CompetitionController {

    private final ICompetitionService competitionService;

    public CompetitionController(ICompetitionService competitionService) {
        this.competitionService = competitionService;
    }

    @GetMapping("/competition/{id}")
    public String getCompetitionEventDetail(@PathVariable("id") Integer id, Model model) {
        try {
            CompetitionEventDetailDTO event = competitionService.getCompetitionEventById(id);
            List<EventImage> eventImages = competitionService.getEventImages(id);
            model.addAttribute("event", event);
            model.addAttribute("eventImages", eventImages);
            
            // Debug info
            System.out.println("=== COMPETITION EVENT DEBUG INFO ===");
            System.out.println("Event loaded: " + (event != null ? event.getTitle() : "NULL"));
            System.out.println("Event capacity: " + (event != null ? event.getCapacity() : "NULL"));
            System.out.println("Event prize: " + (event != null ? event.getPrize() : "NULL"));
            System.out.println("Event rules: " + (event != null ? event.getRules() : "NULL"));
            System.out.println("Event places: " + (event != null && event.getPlaces() != null ? event.getPlaces().size() : 0));
            System.out.println("Event speakers: " + (event != null && event.getSpeakers() != null ? event.getSpeakers().size() : 0));
            System.out.println("Event images count: " + (eventImages != null ? eventImages.size() : 0));
            System.out.println("====================================");
            
        } catch (Exception e) {
            System.err.println("Error loading competition event: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("error", "Không thể tải dữ liệu sự kiện thi đấu: " + e.getMessage());
        }
        return "competition/competitionHome"; // file competitionHome.html
    }
}
