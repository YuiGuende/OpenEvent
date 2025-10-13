package com.group02.openevent.controller.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.group02.openevent.dto.response.EventResponse;
import com.group02.openevent.model.event.Event;
import com.group02.openevent.model.event.Speaker;
import com.group02.openevent.model.event.EventSchedule;
import com.group02.openevent.repository.ISpeakerRepo;
import com.group02.openevent.repository.IEventScheduleRepo;
import com.group02.openevent.service.EventService;
import com.group02.openevent.service.IPlaceService;
import com.group02.openevent.service.impl.PlaceServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Slf4j
@Controller
public class EventManageController {
    @Autowired
    EventService eventService;
    @Autowired
    IPlaceService placeService;
    @Autowired
    ISpeakerRepo speakerRepo;
    @Autowired
    IEventScheduleRepo scheduleRepo;
    @Autowired
    ObjectMapper objectMapper;

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
        model.addAttribute("speakersData", speakersList);
        model.addAttribute("schedulesData", schedulesList);
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
        Event event = eventService.getEventResponseById(id);
        model.addAttribute("event", event);
        model.addAttribute("allPlaces", placeService.getAllByEventId(id));
        model.addAttribute("eventType", event.getClass().getSimpleName());
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

}
