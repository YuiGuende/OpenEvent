package com.group02.openevent.controller.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.group02.openevent.dto.request.update.EventUpdateRequest;
import com.group02.openevent.dto.response.EventResponse;
import com.group02.openevent.mapper.EventMapper;
import com.group02.openevent.model.event.*;
import com.group02.openevent.repository.ISpeakerRepo;
import com.group02.openevent.repository.IEventScheduleRepo;
import com.group02.openevent.repository.IEventImageRepo;
import com.group02.openevent.model.ticket.TicketType;
import com.group02.openevent.service.EventService;
import com.group02.openevent.repository.ITicketTypeRepo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.beans.factory.annotation.Autowired;
import com.group02.openevent.service.PlaceService;
import com.group02.openevent.repository.IPlaceRepo;
import org.springframework.web.bind.annotation.*;
import org.springframework.ui.Model;

import java.util.List;

@Slf4j
@Controller
public class EventManageController {
    @Autowired
    EventService eventService;
    @Autowired
    PlaceService placeService;
    @Autowired
    ISpeakerRepo speakerRepo;
    @Autowired
    IEventScheduleRepo scheduleRepo;
    @Autowired
    IEventImageRepo imageRepo;
    @Autowired
    IPlaceRepo placeRepo;
    @Autowired
    ITicketTypeRepo ticketTypeRepo;
    @Autowired
    EventMapper eventMapper;

    //    @RequestMapping(value = "/manage/event/{eventId:\\d+}/{path:[^\\.]*}")
//    public String forwardSpaRoutes() {
//        // "forward:" là một chỉ thị đặc biệt để Spring thực hiện chuyển tiếp
//        // ở phía server, giữ nguyên URL trên trình duyệt.
//        // Đảm bảo đường dẫn này chính xác.
//        return "forward:/host/manager-event";
//    }
    @RequestMapping(value = "/manage/event/{eventId:\\d+}/{path:[^\\.]*}")
    public String showManagerPage(@PathVariable Long eventId, Model model) throws JsonProcessingException {
        // 1. Lấy dữ liệu Event dựa trên eventId từ URL
        // Phương thức này nên có sẵn logic kiểm tra quyền sở hữu
        Event event = eventService.getEventResponseById(eventId);

        // 2. Đưa dữ liệu Event vào Model để toàn bộ trang có thể sử dụng
        // (ví dụ: hiển thị tên sự kiện ở header)
        model.addAttribute("event", event);

        // 3. Chỉ định fragment mặc định cần tải cho lần đầu tiên
        model.addAttribute("content", "fragments/getting-started :: content");

        List<Speaker> speakersList = speakerRepo.findSpeakerByEventId(eventId);
        List<EventSchedule> schedulesList = scheduleRepo.findByEventId(eventId);
        List<EventImage> imagesList = imageRepo.findByEventId(eventId);
        List<TicketType> ticketTypesList = ticketTypeRepo.findByEventId(eventId);
        model.addAttribute("allPlaces", placeService.getAllByEventId(eventId));
        model.addAttribute("allTicketTypes", ticketTypesList);
        model.addAttribute("speakersData", speakersList);
        model.addAttribute("schedulesData", schedulesList);
        model.addAttribute("imagesData", imagesList);
        
        // 4. Trả về file layout chính
        return "host/manager-event";
    }

    @GetMapping("/host/manager-event")
    public String showMainLayout(Model model) {
        // Thêm nội dung ban đầu cho trang layout (nếu cần)
        model.addAttribute("content", "fragments/getting-started :: content");

        // Dòng này mới thực sự trả về tên file template
        // => /resources/templates/host/manager-event.html
        return "host/manager-event";
    }

    @GetMapping("/fragments/update-event")
    public String updateEvent(@RequestParam Long id, Model model) throws JsonProcessingException {
        log.info("🔍 Loading update-event fragment for event ID: {}", id);
        
        Event event = eventService.getEventResponseById(id);
        EventUpdateRequest request = eventMapper.toUpdateRequest(event);
        model.addAttribute("request", request);
        
        // Load places
        List<Place> allPlaces = placeService.getAllByEventId(id);
        log.info("📋 Places loaded for event {}: {}", id, allPlaces);
        log.info("📋 Places count: {}", allPlaces.size());
        model.addAttribute("allPlaces", allPlaces);
        
        // Load tickets
        List<TicketType> allTicketTypes = ticketTypeRepo.findByEventId(id);
        log.info("🎫 Tickets loaded for event {}: {}", id, allTicketTypes);
        log.info("🎫 Tickets count: {}", allTicketTypes.size());
        model.addAttribute("allTicketTypes", allTicketTypes);
        
        model.addAttribute("eventTypes", event.getClass().getSimpleName());
        
        // Add speakers, schedules, and images data for the fragment
        List<Speaker> speakersList = speakerRepo.findSpeakerByEventId(id);
        List<EventSchedule> schedulesList = scheduleRepo.findByEventId(id);
        List<EventImage> imagesList = imageRepo.findByEventId(id);
        model.addAttribute("speakersData", speakersList);
        model.addAttribute("schedulesData", schedulesList);
        model.addAttribute("imagesData", imagesList);
        
        log.info("✅ All data loaded for update-event fragment");
        return "fragments/update-event :: content";
    }

    @GetMapping("/fragments/getting-started")
    public String gettingStared(@RequestParam Long id, Model model) {
        Event event = eventService.getEventResponseById(id);
        model.addAttribute("event", event);
        return "fragments/getting-started :: content";
    }

    @GetMapping("/fragments/ticket")
    public String ticket(@RequestParam Long id, Model model) {
        Event event = eventService.getEventResponseById(id);
        model.addAttribute("event", event);
        
        // Load tickets for the ticket fragment
        List<TicketType> allTicketTypes = ticketTypeRepo.findByEventId(id);
        model.addAttribute("allTicketTypes", allTicketTypes);
        
        return "fragments/update-ticket :: content";
    }
//    @GetMapping("/fragments/dashboard")
//    public String dashboard(Model model) {
//        return "fragments/dashboard :: content";
//    }
//
//    @GetMapping("/fragments/reports")
//    public String reports(Model model) {
//        return "fragments/reports :: content";
//    }
//
//    @GetMapping("/fragments/attendees")
//    public String attendees(Model model) {
//        return "fragments/attendees :: content";
//    }
//
//    @GetMapping("/fragments/orders")
//    public String orders(Model model) {
//        return "fragments/orders :: content";
//    }


    @GetMapping("/fragments/check-in")
    public String checkIn(@RequestParam Long id, Model model) {
        Event event = eventService.getEventResponseById(id);
        model.addAttribute("event", event);
        return "fragments/check-in :: content";
    }

    // Test endpoint để kiểm tra places
    @GetMapping("/test/places/{eventId}")
    @ResponseBody
    public String testPlaces(@PathVariable Long eventId) {
        log.info("🧪 Testing places for event ID: {}", eventId);
        
        try {
            List<Place> places = placeService.getAllByEventId(eventId);
            log.info("✅ Test successful - Found {} places", places.size());
            
            StringBuilder result = new StringBuilder();
            result.append("Event ID: ").append(eventId).append("\n");
            result.append("Places found: ").append(places.size()).append("\n");
            result.append("Places: ").append(places).append("\n");
            
            return result.toString();
        } catch (Exception e) {
            log.error("❌ Test failed: ", e);
            return "Error: " + e.getMessage();
        }
    }

    // Test endpoint để tạo ticket mẫu
    @GetMapping("/test/create-ticket/{eventId}")
    @ResponseBody
    public String createTestTicket(@PathVariable Long eventId) {
        log.info("🧪 Creating test ticket for event ID: {}", eventId);
        
        try {
            // Get event
            Event event = eventService.getEventById(eventId).orElseThrow(() -> new RuntimeException("Event not found"));
            
            // Create test ticket
            TicketType testTicket = new TicketType();
            testTicket.setName("Vé Early Bird");
            testTicket.setDescription("Vé ưu đãi cho những người đăng ký sớm");
            testTicket.setPrice(new java.math.BigDecimal("100000"));
            testTicket.setTotalQuantity(100);
            testTicket.setSoldQuantity(0);
            testTicket.setSale(new java.math.BigDecimal("10"));
            testTicket.setStartSaleDate(java.time.LocalDateTime.now());
            testTicket.setEndSaleDate(java.time.LocalDateTime.now().plusDays(30));
            testTicket.setEvent(event);
            
            TicketType savedTicket = ticketTypeRepo.save(testTicket);
            log.info("✅ Test ticket created: {}", savedTicket.getName());
            
            return "Test ticket created successfully! ID: " + savedTicket.getTicketTypeId() + ", Name: " + savedTicket.getName();
        } catch (Exception e) {
            log.error("❌ Test failed: ", e);
            return "Error: " + e.getMessage();
        }
    }

}
