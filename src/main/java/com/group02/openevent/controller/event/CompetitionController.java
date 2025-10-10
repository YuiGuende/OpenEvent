package com.group02.openevent.controller.event;

import com.group02.openevent.model.dto.event.CompetitionEventDetailDTO;
import com.group02.openevent.service.ICompetitionService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class CompetitionController {

    private final ICompetitionService competitionService;

    public CompetitionController(ICompetitionService competitionService) {
        this.competitionService = competitionService;
    }

    @GetMapping("/competition/{id}")
    public String getCompetitionEventDetail(@PathVariable("id") Long id, Model model) {
        try {
            CompetitionEventDetailDTO eventDetail = competitionService.getCompetitionEventById(id);

            // DEBUG: Print competition info
            System.out.println("=== COMPETITION EVENT DEBUG ===");
            System.out.println("Event ID: " + id);
            System.out.println("Title: " + eventDetail.getTitle());
            System.out.println("Description: " + eventDetail.getDescription());
            System.out.println("Banner URL: " + eventDetail.getBannerUrl());
            System.out.println("Gallery URLs count: " + (eventDetail.getGalleryUrls() != null ? eventDetail.getGalleryUrls().size() : 0));
            System.out.println("Competition Type: " + eventDetail.getCompetitionType());
            System.out.println("Format: " + eventDetail.getFormat());
            System.out.println("Prize Pool: " + eventDetail.getPrizePool());
            System.out.println("Rules: " + eventDetail.getRules());
            System.out.println("Eligibility: " + eventDetail.getEligibility());
            System.out.println("Judging Criteria: " + eventDetail.getJudgingCriteria());
            System.out.println("Starts At: " + eventDetail.getStartsAt());
            System.out.println("Organization: " + (eventDetail.getOrganization() != null ? eventDetail.getOrganization().getOrgName() : "NULL"));
            System.out.println("Schedules count: " + (eventDetail.getSchedules() != null ? eventDetail.getSchedules().size() : 0));
            System.out.println("==============================");

            model.addAttribute("eventDetail", eventDetail);
        } catch (Exception e) {
            System.err.println("Lỗi khi tải chi tiết cuộc thi ID " + id + ": " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("error", "Không thể tìm thấy cuộc thi bạn yêu cầu.");
        }

        return "competition/competitionHome";
    }
}