package com.group02.openevent.controller.event;

import com.group02.openevent.dto.event.MusicEventDetailDTO;
import com.group02.openevent.model.user.Customer;
import com.group02.openevent.service.IMusicService;
import com.group02.openevent.service.OrderService;
import com.group02.openevent.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class MusicController {

    private final IMusicService musicService;
    private final OrderService orderService;
    private final UserService userService;

    public MusicController(IMusicService musicService, OrderService orderService, UserService userService) {
        this.musicService = musicService;
        this.orderService = orderService;
        this.userService = userService;
    }

    @GetMapping("/music/{id}")
    public String getMusicEventDetail(@PathVariable("id") Long id, Model model, HttpSession session) {
        try {
            // Check if user has already purchased a ticket for this event
            boolean hasPurchasedTicket = false;
            try {
                Customer customer = userService.getCurrentUser(session).getCustomer();
                if (customer != null) {
                    hasPurchasedTicket = orderService.hasCustomerRegisteredForEvent(customer.getCustomerId(), id);
                }
            } catch (Exception e) {
                // User not logged in or not a customer - allow access
                hasPurchasedTicket = false;
            }

            MusicEventDetailDTO eventDetail = musicService.getMusicEventById(id);
            // 2. Truyền DUY NHẤT DTO này sang view với tên là "eventDetail"
            model.addAttribute("eventDetail", eventDetail);
            model.addAttribute("hasPurchasedTicket", hasPurchasedTicket);

        } catch (Exception e) {
            // Xử lý lỗi nếu không tìm thấy sự kiện
            System.err.println("Lỗi khi tải chi tiết sự kiện ID " + id + ": " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("error", "Không thể tìm thấy sự kiện bạn yêu cầu.");
        }

        // 3. Trả về đúng file view
        return "music/musicHome";
    }
}
