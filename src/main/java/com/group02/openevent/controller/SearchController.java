package com.group02.openevent.controller;

import com.group02.openevent.dto.home.EventCardDTO;
import com.group02.openevent.model.enums.EventType;
import com.group02.openevent.service.EventService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.List;
@Controller
public class SearchController {
    @Autowired
    private EventService eventService;

    @GetMapping("/search")
    public String searchEvents(@RequestParam(required = false) String q,
                               @RequestParam(required = false) String type,
                               @RequestParam(required = false)
                               @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                               @RequestParam(required = false)
                               @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                               Model model) {

        List<EventCardDTO> results = eventService.searchEvents(q, type, from, to);
        model.addAttribute("results", results);
        model.addAttribute("query", q);
        return "event/search";
    }
}
