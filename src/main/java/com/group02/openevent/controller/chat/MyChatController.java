package com.group02.openevent.controller.chat;

import com.group02.openevent.model.event.Event;
import com.group02.openevent.model.user.Customer;
import com.group02.openevent.model.user.Host;
import com.group02.openevent.model.user.User;
import com.group02.openevent.model.volunteer.VolunteerApplication;
import com.group02.openevent.model.volunteer.VolunteerStatus;
import com.group02.openevent.repository.ICustomerRepo;
import com.group02.openevent.repository.IHostRepo;
import com.group02.openevent.service.EventService;
import com.group02.openevent.service.UserService;
import com.group02.openevent.service.VolunteerService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Controller
@RequiredArgsConstructor
@Slf4j
public class MyChatController {
    
    private final UserService userService;
    private final EventService eventService;
    private final VolunteerService volunteerService;
    private final IHostRepo hostRepo;
    private final ICustomerRepo customerRepo;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @GetMapping("/my-chat")
    public String myChat(HttpSession session, Model model) {
        try {
            User currentUser = userService.getCurrentUser(session);
            Long userId = currentUser.getUserId();
            
            List<EventChatInfo> eventsWithChat = new ArrayList<>();
            
            // 1. Lấy events mà user là host (user có thể có nhiều host records)
            List<Host> hosts = hostRepo.findAllByUser_UserId(userId);
            for (Host host : hosts) {
                List<Event> hostEvents = eventService.getEventByHostId(host.getId());
                
                for (Event event : hostEvents) {
                    // Kiểm tra đã có trong danh sách chưa (tránh duplicate)
                    boolean alreadyExists = eventsWithChat.stream()
                        .anyMatch(e -> e.getEventId().equals(event.getId()));
                    
                    if (!alreadyExists) {
                        // Thêm tất cả events của host (room sẽ được tạo tự động khi vào chat)
                        eventsWithChat.add(new EventChatInfo(
                            event.getId(),
                            event.getTitle(),
                            event.getImageUrl(),
                            event.getStartsAt(),
                            true, // isHost
                            false // isVolunteer
                        ));
                    }
                }
            }
            
            // 2. Lấy events mà user là approved volunteer
            Optional<Customer> customerOpt = customerRepo.findByUser_UserId(userId);
            if (customerOpt.isPresent()) {
                Customer customer = customerOpt.get();
                List<VolunteerApplication> approvedApplications = 
                    volunteerService.getVolunteerApplicationsByCustomerIdAndStatus(
                        customer.getCustomerId(), VolunteerStatus.APPROVED);
                
                for (VolunteerApplication app : approvedApplications) {
                    Event event = app.getEvent();
                    
                    // Kiểm tra đã có trong danh sách chưa (tránh duplicate - user có thể vừa là host vừa là volunteer)
                    boolean alreadyExists = eventsWithChat.stream()
                        .anyMatch(e -> e.getEventId().equals(event.getId()));
                    
                    if (!alreadyExists) {
                        eventsWithChat.add(new EventChatInfo(
                            event.getId(),
                            event.getTitle(),
                            event.getImageUrl(),
                            event.getStartsAt(),
                            false, // isHost
                            true // isVolunteer
                        ));
                    }
                    // Nếu đã có trong danh sách với vai trò host, giữ nguyên là host
                    // (host có quyền cao hơn volunteer)
                }
            }
            
            // Sắp xếp theo thời gian bắt đầu (mới nhất trước)
            eventsWithChat.sort((a, b) -> {
                if (a.getStartsAt() == null && b.getStartsAt() == null) return 0;
                if (a.getStartsAt() == null) return 1;
                if (b.getStartsAt() == null) return -1;
                return b.getStartsAt().compareTo(a.getStartsAt());
            });
            
            model.addAttribute("events", eventsWithChat);
            model.addAttribute("userId", userId);
            
            // Convert events to JSON for JavaScript
            try {
                String eventsJson = objectMapper.writeValueAsString(eventsWithChat);
                model.addAttribute("eventsJson", eventsJson);
            } catch (Exception e) {
                log.error("Error converting events to JSON", e);
                model.addAttribute("eventsJson", "[]");
            }
            
            return "user/my-chat";
        } catch (Exception e) {
            log.error("Error loading my chat page", e);
            return "redirect:/login?redirectUrl=/my-chat";
        }
    }
    
    // Inner class để chứa thông tin event với chat
    public static class EventChatInfo {
        private Long eventId;
        private String eventTitle;
        private String eventImageUrl;
        private java.time.LocalDateTime startsAt;
        private boolean isHost;
        private boolean isVolunteer;
        
        public EventChatInfo(Long eventId, String eventTitle, String eventImageUrl, 
                           java.time.LocalDateTime startsAt, boolean isHost, boolean isVolunteer) {
            this.eventId = eventId;
            this.eventTitle = eventTitle;
            this.eventImageUrl = eventImageUrl;
            this.startsAt = startsAt;
            this.isHost = isHost;
            this.isVolunteer = isVolunteer;
        }
        
        public Long getEventId() { return eventId; }
        public String getEventTitle() { return eventTitle; }
        public String getEventImageUrl() { return eventImageUrl; }
        public java.time.LocalDateTime getStartsAt() { return startsAt; }
        public boolean isHost() { return isHost; }
        public boolean isVolunteer() { return isVolunteer; }
        
        public String getRole() {
            if (isHost) return "Host";
            if (isVolunteer) return "Volunteer";
            return "Participant";
        }
    }
}

