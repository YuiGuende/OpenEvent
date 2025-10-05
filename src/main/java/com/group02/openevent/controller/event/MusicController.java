package com.group02.openevent.controller.event;

import com.group02.openevent.model.dto.music.MusicEventDetailDTO;
import com.group02.openevent.model.event.EventImage;
import com.group02.openevent.service.IMusicService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@Controller
public class MusicController {

    private final IMusicService musicService;


    public MusicController(IMusicService musicService) {
        this.musicService = musicService;
    }

    @GetMapping("/music/{id}")
    public String getMusicEventDetail(@PathVariable("id") Long id, Model model) {
        try {
            MusicEventDetailDTO event = musicService.getMusicEventById(id);
            List<EventImage> eventImages = musicService.getEventImages(id);
            model.addAttribute("event", event); // truyền 1 object duy nhất
            model.addAttribute("eventImages", eventImages);
            
            // Debug info
            System.out.println("=== EVENT DEBUG INFO ===");
            System.out.println("Event loaded: " + (event != null ? event.getTitle() : "NULL"));
            System.out.println("Event capacity: " + (event != null ? event.getCapacity() : "NULL"));
            System.out.println("Event places: " + (event != null && event.getPlaces() != null ? event.getPlaces().size() : 0));
            System.out.println("Event benefits: " + (event != null ? event.getBenefits() : "NULL"));
            System.out.println("Event startsAt: " + (event != null ? event.getStartsAt() : "NULL"));
            System.out.println("Event endsAt: " + (event != null ? event.getEndsAt() : "NULL"));
            System.out.println("Event images count: " + (eventImages != null ? eventImages.size() : 0));
            System.out.println("========================");
            
        } catch (Exception e) {
            System.err.println("Error loading event: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("error", "Không thể tải dữ liệu sự kiện: " + e.getMessage());
        }
        return "music/musicHome"; // file musicHome.html
    }
}
