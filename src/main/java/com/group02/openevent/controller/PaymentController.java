//package com.group02.openevent.controller;
//
//import com.group02.openevent.model.account.Account;
//import com.group02.openevent.model.user.User;
//import com.group02.openevent.model.payment.Payment;
//import com.group02.openevent.model.ticket.Ticket;
//import com.group02.openevent.repository.IAccountRepo;
//import com.group02.openevent.repository.IUserRepo;
//import com.group02.openevent.service.TicketService;
//import com.group02.openevent.service.PaymentService;
//import com.group02.openevent.dto.ticket.CreateTicketRequest;
//import com.group02.openevent.dto.payment.PaymentResult;
//import com.group02.openevent.dto.payment.PayOSWebhookData;
//import jakarta.servlet.http.HttpSession;
//import org.springframework.http.ResponseEntity;
//import org.springframework.stereotype.Controller;
//import org.springframework.web.bind.annotation.*;
//import vn.payos.PayOS;
//import vn.payos.type.PaymentData;
//import vn.payos.type.Webhook;
//import vn.payos.type.WebhookData;
//
//import java.util.HashMap;
//import java.util.Map;
//import java.util.Optional;
//
//@Controller
//@RequestMapping("/api/payments")
//public class PaymentController {
//
//    private final PaymentService paymentService;
//    private final TicketService ticketService;
//    private final IAccountRepo accountRepo;
//    private final IUserRepo userRepo;
//    private final PayOS payOS;
//
//    public PaymentController(PaymentService paymentService, TicketService ticketService,
//                           IAccountRepo accountRepo, IUserRepo userRepo, PayOS payOS) {
//        this.paymentService = paymentService;
//        this.ticketService = ticketService;
//        this.accountRepo = accountRepo;
//        this.userRepo = userRepo;
//        this.payOS = payOS;
//    }
//
//    /**
//     * Tạo ticket và payment link sử dụng PayOS SDK
//     */
//    @PostMapping("/create")
//    @ResponseBody
//    public ResponseEntity<Map<String, Object>> createPayment(@RequestBody CreateTicketRequest request,
//                                                           HttpSession session) {
//        try {
//            Long accountId = (Long) session.getAttribute("ACCOUNT_ID");
//            if (accountId == null) {
//                return ResponseEntity.badRequest().body(Map.of(
//                    "success", false,
//                    "message", "User not logged in"
//                ));
//            }
//
//            // Get user
//            Account account = accountRepo.findById(accountId).orElse(null);
//            if (account == null) {
//                return ResponseEntity.badRequest().body(Map.of(
//                    "success", false,
//                    "message", "Account not found"
//                ));
//            }
//
//            User user = userRepo.findByAccount(account).orElse(null);
//            if (user == null) {
//                return ResponseEntity.badRequest().body(Map.of(
//                    "success", false,
//                    "message", "User not found"
//                ));
//            }
//
//            // Create ticket
//            Ticket ticket = ticketService.createTicket(request, user);
//
//            // Create payment link using PayOS SDK
//            String returnUrl = "http://localhost:8080/payment/success?ticketCode=" + ticket.getTicketCode();
//            String cancelUrl = "http://localhost:8080/payment/cancel?ticketCode=" + ticket.getTicketCode();
//
//            // Sử dụng PayOS SDK để tạo payment link
//            PaymentData paymentData = PaymentData.builder()
//                .orderCode((long) Math.abs(ticket.getTicketCode().hashCode()))
//                .amount(ticket.getPrice().intValue())
//                .description(ticket.getDescription())
//                .items(null)
//                .returnUrl(returnUrl)
//                .cancelUrl(cancelUrl)
//                .build();
//
//            var paymentLink = payOS.createPaymentLink(paymentData);
//
//            Map<String, Object> response = new HashMap<>();
//            response.put("success", true);
//            response.put("ticketCode", ticket.getTicketCode());
//            response.put("paymentUrl", paymentLink.getCheckoutUrl());
//            response.put("checkoutUrl", paymentLink.getCheckoutUrl());
//            response.put("qrCode", paymentLink.getQrCode());
//            response.put("amount", paymentLink.getAmount());
//
//            return ResponseEntity.ok(response);
//
//        } catch (Exception e) {
//            e.printStackTrace();
//            return ResponseEntity.badRequest().body(Map.of(
//                "success", false,
//                "message", e.getMessage()
//            ));
//        }
//    }
//
//    /**
//     * Webhook endpoint cho PayOS sử dụng PayOS SDK
//     */
//    @PostMapping("/webhook")
//    @ResponseBody
//    public ResponseEntity<Map<String, Object>> handleWebhook(@RequestBody Webhook webhookData) {
//        try {
//            // Verify webhook using PayOS SDK
//            WebhookData data = payOS.verifyPaymentWebhookData(webhookData);
//            System.out.println("Webhook verified: " + data);
//
//            // Process payment result using PayOS SDK webhook
//            PaymentResult result = paymentService.handlePaymentWebhookFromPayOS(convertWebhookData(data));
//
//            Map<String, Object> response = new HashMap<>();
//            response.put("error", 0);
//            response.put("message", "Webhook delivered");
//            response.put("success", result.isSuccess());
//
//            return ResponseEntity.ok(response);
//
//        } catch (Exception e) {
//            e.printStackTrace();
//            Map<String, Object> response = new HashMap<>();
//            response.put("error", -1);
//            response.put("message", e.getMessage());
//            response.put("success", false);
//            return ResponseEntity.badRequest().body(response);
//        }
//    }
//
//    /**
//     * Lấy thông tin payment theo ticket code
//     */
//    @GetMapping("/status/{ticketCode}")
//    @ResponseBody
//    public ResponseEntity<Map<String, Object>> getPaymentStatus(@PathVariable String ticketCode) {
//        try {
//            Optional<Ticket> ticketOpt = ticketService.getTicketByCode(ticketCode);
//            if (ticketOpt.isEmpty()) {
//                return ResponseEntity.badRequest().body(Map.of(
//                    "success", false,
//                    "message", "Ticket not found"
//                ));
//            }
//
//            Ticket ticket = ticketOpt.get();
//            Optional<Payment> paymentOpt = paymentService.getPaymentByTicket(ticket);
//
//            Map<String, Object> response = new HashMap<>();
//            response.put("success", true);
//            response.put("ticketCode", ticket.getTicketCode());
//            response.put("ticketStatus", ticket.getStatus().name());
//
//            if (paymentOpt.isPresent()) {
//                Payment payment = paymentOpt.get();
//                response.put("paymentStatus", payment.getStatus().name());
//                response.put("checkoutUrl", payment.getCheckoutUrl());
//                response.put("qrCode", payment.getQrCode());
//            }
//
//            return ResponseEntity.ok(response);
//
//        } catch (Exception e) {
//            return ResponseEntity.badRequest().body(Map.of(
//                "success", false,
//                "message", e.getMessage()
//            ));
//        }
//    }
//
//    /**
//     * Lấy lịch sử payments của user
//     */
//    @GetMapping("/history")
//    @ResponseBody
//    public ResponseEntity<Map<String, Object>> getPaymentHistory(HttpSession session) {
//        try {
//            Long accountId = (Long) session.getAttribute("ACCOUNT_ID");
//            if (accountId == null) {
//                return ResponseEntity.badRequest().body(Map.of(
//                    "success", false,
//                    "message", "User not logged in"
//                ));
//            }
//
//            var payments = paymentService.getPaymentsByUserId(accountId);
//
//            Map<String, Object> response = new HashMap<>();
//            response.put("success", true);
//            response.put("payments", payments);
//
//            return ResponseEntity.ok(response);
//
//        } catch (Exception e) {
//            return ResponseEntity.badRequest().body(Map.of(
//                "success", false,
//                "message", e.getMessage()
//            ));
//        }
//    }
//
//    /**
//     * Hủy payment
//     */
//    @PostMapping("/cancel/{ticketCode}")
//    @ResponseBody
//    public ResponseEntity<Map<String, Object>> cancelPayment(@PathVariable String ticketCode) {
//        try {
//            Optional<Ticket> ticketOpt = ticketService.getTicketByCode(ticketCode);
//            if (ticketOpt.isEmpty()) {
//                return ResponseEntity.badRequest().body(Map.of(
//                    "success", false,
//                    "message", "Ticket not found"
//                ));
//            }
//
//            Ticket ticket = ticketOpt.get();
//            Optional<Payment> paymentOpt = paymentService.getPaymentByTicket(ticket);
//
//            if (paymentOpt.isEmpty()) {
//                return ResponseEntity.badRequest().body(Map.of(
//                    "success", false,
//                    "message", "Payment not found"
//                ));
//            }
//
//            Payment payment = paymentOpt.get();
//            boolean cancelled = paymentService.cancelPayment(payment);
//
//            Map<String, Object> response = new HashMap<>();
//            response.put("success", cancelled);
//            response.put("message", cancelled ? "Payment cancelled successfully" : "Cannot cancel payment");
//
//            return ResponseEntity.ok(response);
//
//        } catch (Exception e) {
//            return ResponseEntity.badRequest().body(Map.of(
//                "success", false,
//                "message", e.getMessage()
//            ));
//        }
//    }
//
//    /**
//     * Convert PayOS SDK WebhookData to PayOSWebhookData
//     */
//    private PayOSWebhookData convertWebhookData(WebhookData webhookData) {
//        PayOSWebhookData payOSWebhookData = new PayOSWebhookData();
//        payOSWebhookData.setCode(Integer.valueOf(webhookData.getCode()));
//        payOSWebhookData.setDesc(webhookData.getDesc());
//
//        // Create minimal data structure
//        PayOSWebhookData.Data data = new PayOSWebhookData.Data();
//        // Note: WebhookData from PayOS SDK has different structure
//        // We'll create a minimal structure for now
//        // Since we can't access getData(), we'll use default values
//        data.setOrderCode(0L); // Default value
//        data.setAmount(0); // Default value
//        data.setDescription("Webhook from PayOS SDK");
//        data.setCode("00"); // Default success code
//        data.setDesc("Success");
//
//        payOSWebhookData.setData(data);
//        return payOSWebhookData;
//    }
//}
