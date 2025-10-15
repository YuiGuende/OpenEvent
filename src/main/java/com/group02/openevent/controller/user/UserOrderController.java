package com.group02.openevent.controller.user;

import com.group02.openevent.dto.user.UserOrderDTO;
import com.group02.openevent.model.order.OrderStatus;
import com.group02.openevent.model.user.Customer;
import com.group02.openevent.repository.ICustomerRepo;
import com.group02.openevent.service.OrderService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequestMapping
public class UserOrderController {

    private final OrderService orderService;
    private final ICustomerRepo customerRepo;

    public UserOrderController(OrderService orderService, ICustomerRepo customerRepo) {
        this.orderService = orderService;
        this.customerRepo = customerRepo;
    }

    /**
     * GET /orders - Hiển thị danh sách đơn hàng của user hiện tại
     */
    @GetMapping("/orders")
    public String viewOrders(
            HttpSession session,
            @RequestParam(required = false) OrderStatus status,
            Model model) {

        Long accountId = (Long) session.getAttribute("ACCOUNT_ID");
        if (accountId == null) {
            return "redirect:/login";
        }

        Customer customer = customerRepo.findByAccount_AccountId(accountId)
                .orElse(null);
        if (customer == null) {
            model.addAttribute("error", "Customer not found");
            return "error/404";
        }

        List<UserOrderDTO> orders = orderService.getOrderDTOsByCustomer(customer, status);

        model.addAttribute("orders", orders);
        model.addAttribute("orderStatuses", OrderStatus.values());
        model.addAttribute("selectedStatus", status);

        long totalOrders = orders.size();
        long paidOrders = orders.stream().filter(o -> o.getStatus() == OrderStatus.PAID).count();
        long pendingOrders = orders.stream().filter(o -> o.getStatus() == OrderStatus.PENDING).count();

        model.addAttribute("totalOrders", totalOrders);
        model.addAttribute("paidOrders", paidOrders);
        model.addAttribute("pendingOrders", pendingOrders);

        return "user/my-orders";
    }

    /**
     * GET /orders/{orderId} - Chi tiết một đơn hàng (chỉ cho chủ sở hữu)
     */
    @GetMapping("/orders/{orderId}")
    public String viewOrderDetail(
            @PathVariable Long orderId,
            HttpSession session,
            Model model) {

        Long accountId = (Long) session.getAttribute("ACCOUNT_ID");
        if (accountId == null) {
            return "redirect:/login";
        }

        Customer customer = customerRepo.findByAccount_AccountId(accountId).orElse(null);
        if (customer == null) {
            model.addAttribute("error", "Customer not found");
            return "error/404";
        }

        var orderOpt = orderService.getById(orderId);
        if (orderOpt.isEmpty()) {
            return "error/404";
        }

        var order = orderOpt.get();
        if (order.getCustomer() == null || !order.getCustomer().getCustomerId().equals(customer.getCustomerId())) {
            return "error/404"; // Access denied or not found
        }

        // Map to lightweight DTO for the view
        UserOrderDTO dto = UserOrderDTO.builder()
                .orderId(order.getOrderId())
                .status(order.getStatus())
                .createdAt(order.getCreatedAt())
                .eventId(order.getEvent() != null ? order.getEvent().getId() : null)
                .eventTitle(order.getEvent() != null ? order.getEvent().getTitle() : null)
                .eventImageUrl(order.getEvent() != null ? order.getEvent().getImageUrl() : null)
                .eventStartsAt(order.getEvent() != null ? order.getEvent().getStartsAt() : null)
                .ticketTypeId(order.getTicketType() != null ? order.getTicketType().getTicketTypeId() : null)
                .ticketTypeName(order.getTicketType() != null ? order.getTicketType().getName() : null)
                .totalAmount(order.getTotalAmount())
                .build();

        model.addAttribute("order", dto);
        return "user/order-detail";
    }
}


