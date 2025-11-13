package com.group02.openevent.controller.chat;

import com.group02.openevent.model.user.User;
import com.group02.openevent.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/department")
public class DepartmentChatController {
    
    private final UserService userService;
    
    public DepartmentChatController(UserService userService) {
        this.userService = userService;
    }
    
    @GetMapping("/chat")
    public String departmentChat(HttpSession session, Model model) {
        try {
            User currentUser = userService.getCurrentUser(session);
            
            // Department chat không cần eventId cụ thể
            // Sử dụng eventId = 0 hoặc null để frontend biết đây là department chat
            // Frontend sẽ load tất cả HOST_DEPARTMENT rooms
            model.addAttribute("eventId", 0); // Dummy value để frontend hoạt động
            model.addAttribute("uid", currentUser.getUserId());
            model.addAttribute("eventTitle", "Chat với Host");
            model.addAttribute("isHost", false);
            model.addAttribute("isDepartment", true); // Flag để biết đây là department chat
            
            return "department/chat";
        } catch (Exception e) {
            // User not logged in, redirect to login
            return "redirect:/login?redirectUrl=/department/chat";
        }
    }
}

