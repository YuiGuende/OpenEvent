package com.group02.openevent.controller;

import com.group02.openevent.model.order.Order;
import com.group02.openevent.service.OrderService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Optional;

@Controller
public class PaymentPageController {

    private final OrderService orderService;

    public PaymentPageController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping("/payment/success")
    public String paymentSuccess(@RequestParam(required = false) String ticketCode,
                                 @RequestParam(required = false) Long orderId,
                                 Model model) {
        
        if (orderId != null) {
            Optional<Order> orderOpt = orderService.getById(orderId);
            if (orderOpt.isPresent()) {
                Order order = orderOpt.get();
                model.addAttribute("order", order);
                model.addAttribute("successMessage", "Payment successful! Your order has been confirmed.");
            } else {
                model.addAttribute("errorMessage", "Order not found.");
            }
        } else if (ticketCode != null) {
            // Legacy support for ticketCode parameter
            model.addAttribute("successMessage", "Payment successful! (Legacy ticket code: " + ticketCode + ")");
        } else {
            model.addAttribute("successMessage", "Payment successful!");
        }
        
        return "payment/success";
    }

    @GetMapping("/payment/cancel")
    public String paymentCancel(@RequestParam(required = false) String ticketCode,
                               @RequestParam(required = false) Long orderId,
                               Model model) {
        if (orderId != null) {
            model.addAttribute("errorMessage", "Payment cancelled. Order ID: " + orderId);
        } else if (ticketCode != null) {
            model.addAttribute("errorMessage", "Payment cancelled. (Legacy ticket code: " + ticketCode + ")");
        } else {
            model.addAttribute("errorMessage", "Payment cancelled.");
        }
        return "payment/cancel";
    }

    @GetMapping("/payment/mock-success")
    public String mockPaymentSuccess(@RequestParam(required = false) String ticketCode,
                                   Model model) {
        if (ticketCode != null) {
            model.addAttribute("ticketCode", ticketCode);
        }
        return "payment/mock-success";
    }
}
