package com.group02.openevent.controller.event;

import com.group02.openevent.dto.event.CompetitionEventDetailDTO;
import com.group02.openevent.service.ICompetitionService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/competition")
public class CompetitionRestController {
    private final ICompetitionService competitionService;

    public CompetitionRestController(ICompetitionService competitionService) {
        this.competitionService = competitionService;
    }
    //lay all competition
    @GetMapping
    public List<CompetitionEventDetailDTO> getAllCompetitions(){
        return competitionService.getAllCompetitionEvents();
    }

    @GetMapping("/{id}")
    public CompetitionEventDetailDTO getCompetitionById(@PathVariable Long id) {
        return competitionService.getCompetitionEventById(id);
    }
}
