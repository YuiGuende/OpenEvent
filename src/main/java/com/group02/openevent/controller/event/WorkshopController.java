//package com.group02.openevent.controller.event;
//
//import com.group02.openevent.model.dto.workshop.WorkshopEventDetailDTO;
//import com.group02.openevent.model.event.EventImage;
//import com.group02.openevent.service.IWorkshopService;
//import org.springframework.stereotype.Controller;
//import org.springframework.ui.Model;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.PathVariable;
//
//import java.util.List;
//
//@Controller
//public class WorkshopController {
//
//    private final IWorkshopService workshopService;
//
//    public WorkshopController(IWorkshopService workshopService) {
//        this.workshopService = workshopService;
//    }
//
//    @GetMapping("/workshop/{id}")
//    public String getWorkshopEventDetail(@PathVariable("id") Integer id, Model model) {
//        try {
//            WorkshopEventDetailDTO event = workshopService.getWorkshopEventById(id);
//            List<EventImage> eventImages = workshopService.getEventImages(id);
//            model.addAttribute("event", event);
//            model.addAttribute("eventImages", eventImages);
//
//            // Debug info
//            System.out.println("=== WORKSHOP EVENT DEBUG INFO ===");
//            System.out.println("Event loaded: " + (event != null ? event.getTitle() : "NULL"));
//            System.out.println("Event capacity: " + (event != null ? event.getCapacity() : "NULL"));
//            System.out.println("Event materials: " + (event != null ? event.getMaterials() : "NULL"));
//            System.out.println("Event prerequisites: " + (event != null ? event.getPrerequisites() : "NULL"));
//            System.out.println("Event skill level: " + (event != null ? event.getSkillLevel() : "NULL"));
//            System.out.println("Event places: " + (event != null && event.getPlaces() != null ? event.getPlaces().size() : 0));
//            System.out.println("Event speakers: " + (event != null && event.getSpeakers() != null ? event.getSpeakers().size() : 0));
//            System.out.println("Event images count: " + (eventImages != null ? eventImages.size() : 0));
//            System.out.println("=================================");
//
//        } catch (Exception e) {
//            System.err.println("Error loading workshop event: " + e.getMessage());
//            e.printStackTrace();
//            model.addAttribute("error", "Không thể tải dữ liệu sự kiện workshop: " + e.getMessage());
//        }
//        return "workshop/workshopHome"; // file workshopHome.html
//    }
//}
