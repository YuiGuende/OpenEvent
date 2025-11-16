package com.group02.openevent.ai.service;

import com.group02.openevent.ai.dto.PendingOrder;
import com.group02.openevent.dto.order.CreateOrderWithTicketTypeRequest;
import com.group02.openevent.model.event.Event;
import com.group02.openevent.model.order.Order;
import com.group02.openevent.model.payment.Payment;
import com.group02.openevent.model.payment.PaymentStatus;
import com.group02.openevent.model.ticket.TicketType;
import com.group02.openevent.model.user.Customer;
import com.group02.openevent.repository.ICustomerRepo;
import com.group02.openevent.service.EventService;
import com.group02.openevent.service.OrderService;
import com.group02.openevent.service.PaymentService;
import com.group02.openevent.service.TicketTypeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Service to handle AI-driven order creation
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderAIService {

    private final EventService eventService;
    private final TicketTypeService ticketTypeService;
    private final OrderService orderService;
    private final PaymentService paymentService;
    private final ICustomerRepo customerRepo;
    private final AgentEventService agentEventService;

    // Store pending orders by userId
    private final Map<Long, PendingOrder> pendingOrders = new HashMap<>();

    /**
     * Start order creation process
     */
    public String startOrderCreation(Long userId, String eventQuery) {
        // Tìm sự kiện PUBLIC theo tên
        List<Event> events = eventService.findByTitleAndPublicStatus(eventQuery);

        if (events.isEmpty()) {
            return "❌ Em chưa tìm thấy sự kiện có tên \"" + eventQuery + "\" trên hệ thống ạ.\n"
                    + "Anh/chị giúp em kiểm tra lại tên sự kiện hoặc gõ rõ hơn một chút được không? 😊";
        }

        // Tạm thời lấy sự kiện khớp đầu tiên
        Event event = events.get(0);

        // Lấy danh sách vé THẬT từ DB
        List<TicketType> ticketTypes = ticketTypeService.getTicketTypesByEventId(event.getId());

        if (ticketTypes.isEmpty()) {
            return "ℹ️ Sự kiện **" + event.getTitle() + "** hiện vẫn chưa cấu hình/mở bán bất kỳ loại vé nào trên hệ thống ạ.\n"
                    + "Anh/chị có thể chọn sự kiện khác hoặc quay lại sau nhé! 😊";
        }

        // Tạo pending order cho user
        PendingOrder pendingOrder = new PendingOrder();
        pendingOrder.setEvent(event);
        pendingOrder.setCurrentStep(PendingOrder.OrderStep.SELECT_TICKET_TYPE);
        pendingOrders.put(userId, pendingOrder);

        // Format thời gian & tiền
        DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        NumberFormat moneyFmt = NumberFormat.getInstance(new Locale("vi", "VN"));

        StringBuilder response = new StringBuilder();
        response.append("Dạ em đã tìm thấy sự kiện **\"")
                .append(event.getTitle())
                .append("\"** rồi ạ! 🎉\n\n");

        if (event.getStartsAt() != null && event.getEndsAt() != null) {
            response.append("📅 Thời gian: ")
                    .append(event.getStartsAt().format(timeFmt))
                    .append(" ➜ ")
                    .append(event.getEndsAt().format(timeFmt))
                    .append("\n\n");
        }

        response.append("Hiện tại sự kiện đang có các loại vé sau:\n\n");

        for (TicketType ticket : ticketTypes) {
            response.append("• **").append(ticket.getName()).append("**\n");

            if (ticket.getFinalPrice() != null) {
                long price = ticket.getFinalPrice().longValue();
                response.append("  - Giá: ")
                        .append(moneyFmt.format(price))
                        .append(" VNĐ/vé\n");
            } else {
                response.append("  - Giá: đang cập nhật\n");
            }

            response.append("  - Còn lại: ")
                    .append(ticket.getAvailableQuantity())
                    .append(" vé");

            if (!ticket.isAvailable()) {
                response.append(" (⛔ tạm hết)");
            }

            response.append("\n");

            if (ticket.getDescription() != null && !ticket.getDescription().isBlank()) {
                response.append("  - Mô tả: ")
                        .append(ticket.getDescription().trim())
                        .append("\n");
            }

            response.append("\n");
        }

        response.append("Anh/chị muốn chọn **loại vé nào** và **số lượng bao nhiêu** ạ? 😊");

        return response.toString();
    }

    /**
     * Select ticket type
     */
    public String selectTicketType(Long userId, String ticketTypeName) {
        PendingOrder pendingOrder = pendingOrders.get(userId);

        if (pendingOrder == null || pendingOrder.getEvent() == null) {
            return "❌ Vui lòng chọn sự kiện trước. Bạn có thể nói: 'Mua vé sự kiện [tên sự kiện]'";
        }

        // Find ticket type
        List<TicketType> ticketTypes = ticketTypeService.getTicketTypesByEventId(pendingOrder.getEvent().getId());
        Optional<TicketType> selectedTicket = ticketTypes.stream()
                .filter(t -> t.getName().toLowerCase().contains(ticketTypeName.toLowerCase()))
                .findFirst();

        if (selectedTicket.isEmpty()) {
            return "❌ Không tìm thấy loại vé \"" + ticketTypeName + "\". Các loại vé có sẵn: "
                    + String.join(", ", ticketTypes.stream().map(TicketType::getName).toList());
        }

        TicketType ticket = selectedTicket.get();

        // Check availability
        if (!ticket.isAvailable()) {
            return "❌ Loại vé \"" + ticket.getName() + "\" đã hết. Vui lòng chọn loại vé khác.";
        }

        pendingOrder.setTicketType(ticket);
        pendingOrder.setCurrentStep(PendingOrder.OrderStep.PROVIDE_INFO);

        return "✅ Đã chọn vé **" + ticket.getName() + "** - Giá: " + ticket.getFinalPrice() + " VND\n\n" +
               "📝 Vui lòng cung cấp thông tin:\n" +
               "- Tên người tham gia\n" +
               "- Email\n" +
               "- Số điện thoại (tùy chọn)\n\n" +
               "Ví dụ: 'Tên: Nguyễn Văn A, Email: test@gmail.com, SĐT: 0123456789'";
    }

    /**
     * Provide participant information
     */
    public String provideInfo(Long userId, Map<String, String> info) {
        PendingOrder pendingOrder = pendingOrders.get(userId);

        if (pendingOrder == null || pendingOrder.getTicketType() == null) {
            return "❌ Vui lòng chọn loại vé trước.";
        }

        // Extract information
        if (info.containsKey("name")) pendingOrder.setParticipantName(info.get("name"));
        if (info.containsKey("email")) pendingOrder.setParticipantEmail(info.get("email"));
        if (info.containsKey("phone")) pendingOrder.setParticipantPhone(info.get("phone"));
        if (info.containsKey("organization")) pendingOrder.setParticipantOrganization(info.get("organization"));
        if (info.containsKey("notes")) pendingOrder.setNotes(info.get("notes"));

        // Check if complete
        if (!pendingOrder.isComplete()) {
            return "⚠️ Còn thiếu thông tin:\n" + pendingOrder.getMissingFields() +
                   "\nVui lòng cung cấp đầy đủ thông tin.";
        }

        pendingOrder.setCurrentStep(PendingOrder.OrderStep.CONFIRM_ORDER);

        // Show summary
        StringBuilder summary = new StringBuilder();
        summary.append("📋 **Xác nhận thông tin đơn hàng:**\n\n");
        summary.append("🎫 Sự kiện: ").append(pendingOrder.getEvent().getTitle()).append("\n");
        summary.append("🎟️ Loại vé: ").append(pendingOrder.getTicketType().getName()).append("\n");
        summary.append("💰 Giá: ").append(pendingOrder.getTicketType().getFinalPrice()).append(" VND\n\n");
        summary.append("👤 Thông tin người tham gia:\n");
        summary.append("- Tên: ").append(pendingOrder.getParticipantName()).append("\n");
        summary.append("- Email: ").append(pendingOrder.getParticipantEmail()).append("\n");
        if (pendingOrder.getParticipantPhone() != null) {
            summary.append("- SĐT: ").append(pendingOrder.getParticipantPhone()).append("\n");
        }
        summary.append("\n💡 Xác nhận đặt vé? (Có/Không)");

        return summary.toString();
    }

    /**
     * Confirm and create order
     */
    @Transactional
    public Map<String, Object> confirmOrder(Long userId) {
        log.info("🔍 DEBUG: Starting confirmOrder for userId: {}", userId);

        PendingOrder pendingOrder = pendingOrders.get(userId);
        log.info("🔍 DEBUG: Found pending order: {}", pendingOrder != null ? "YES" : "NO");

        Map<String, Object> result = new HashMap<>();

        if (pendingOrder == null || !pendingOrder.isComplete()) {
            log.error("❌ DEBUG: Pending order incomplete. pendingOrder={}, isComplete={}",
                    pendingOrder != null, pendingOrder != null ? pendingOrder.isComplete() : false);
            result.put("success", false);
            result.put("message", "❌ Thông tin đơn hàng không đầy đủ.");
            return result;
        }

        log.info("🔍 DEBUG: Pending order details - Event: {}, TicketType: {}, Participant: {}",
                pendingOrder.getEvent() != null ? pendingOrder.getEvent().getTitle() : "NULL",
                pendingOrder.getTicketType() != null ? pendingOrder.getTicketType().getName() : "NULL",
                pendingOrder.getParticipantName());

        try {
            // Get customer
            log.info("🔍 DEBUG: Looking for customer with userId: {}", userId);
            Optional<Customer> customerOpt = customerRepo.findByUser_Account_AccountId(userId);
            if (customerOpt.isEmpty()) {
                log.error("❌ DEBUG: Customer not found for userId: {}", userId);
                result.put("success", false);
                result.put("message", "❌ Không tìm thấy thông tin khách hàng.");
                return result;
            }

            Customer customer = customerOpt.get();
            String email = customer.getUser() != null && customer.getUser().getAccount() != null 
                ? customer.getUser().getAccount().getEmail() : "No email";
            log.info("🔍 DEBUG: Customer found - customerId: {}, email: {}",
                    customer.getCustomerId(), email);

            // RE-VALIDATE ticket availability from database before creating order
            // This prevents race conditions where ticket was sold between selection and confirmation
            Long ticketTypeId = pendingOrder.getTicketType().getTicketTypeId();
            TicketType ticketType = ticketTypeService.getTicketTypeById(ticketTypeId)
                    .orElseThrow(() -> new IllegalStateException("Ticket type not found: " + ticketTypeId));
            
            if (!ticketType.isAvailable() || !ticketTypeService.canPurchaseTickets(ticketTypeId, 1)) {
                pendingOrders.remove(userId);
                log.warn("⚠️ Ticket type {} is no longer available when confirming order for user {}", 
                        ticketTypeId, userId);
                result.put("success", false);
                result.put("message", "❌ Loại vé này đã hết. Vui lòng chọn loại vé khác.");
                return result;
            }
            
            // Check if event is still open for registration
            Event event = pendingOrder.getEvent();
            if (event.getStatus() != com.group02.openevent.model.enums.EventStatus.PUBLIC) {
                pendingOrders.remove(userId);
                log.warn("⚠️ Event {} is not open for registration when confirming order", event.getId());
                result.put("success", false);
                result.put("message", "❌ Sự kiện này hiện không mở đăng ký. Vui lòng chọn sự kiện khác.");
                return result;
            }
            
            if (event.getStartsAt() != null && event.getStartsAt().isBefore(java.time.LocalDateTime.now())) {
                pendingOrders.remove(userId);
                log.warn("⚠️ Event {} has already started when confirming order", event.getId());
                result.put("success", false);
                result.put("message", "❌ Sự kiện này đã bắt đầu. Không thể đăng ký.");
                return result;
            }

            // Create order request
            CreateOrderWithTicketTypeRequest request = new CreateOrderWithTicketTypeRequest();
            request.setEventId(pendingOrder.getEvent().getId());
            request.setParticipantName(pendingOrder.getParticipantName());
            request.setParticipantEmail(pendingOrder.getParticipantEmail());
            request.setParticipantPhone(pendingOrder.getParticipantPhone());
            request.setParticipantOrganization(pendingOrder.getParticipantOrganization());
            request.setNotes(pendingOrder.getNotes());
            request.setTicketTypeId(pendingOrder.getTicketType().getTicketTypeId());

            // Create order
            log.info("🔍 DEBUG: Creating order with OrderService...");
            Order order = orderService.createOrderWithTicketTypes(request, customer);
            log.info("🔍 DEBUG: Order created successfully - orderId: {}, status: {}",
                    order.getOrderId(), order.getStatus());

            // Create payment link (or free payment if amount = 0)
            log.info("🔍 DEBUG: Creating payment link...");
            String returnUrl = "http://localhost:8080/payment/success?orderId=" + order.getOrderId();
            String cancelUrl = "http://localhost:8080/payment/cancel?orderId=" + order.getOrderId();
            Payment payment = paymentService.createPaymentLinkForOrder(order, returnUrl, cancelUrl);
            log.info("🔍 DEBUG: Payment created successfully - paymentId: {}, status: {}",
                    payment.getPaymentId(), payment.getStatus());

            // Clear pending order
            pendingOrders.remove(userId);
            log.info("🔍 DEBUG: Pending order cleared for userId: {}", userId);

            // Check if this is a free event
            boolean isFreeEvent = payment.getStatus() == PaymentStatus.PAID && 
                                  payment.getAmount().compareTo(java.math.BigDecimal.ZERO) == 0;

            // Return success
            result.put("success", true);
            result.put("orderId", order.getOrderId());
            result.put("amount", payment.getAmount());
            result.put("isFreeEvent", isFreeEvent);
            
            if (isFreeEvent) {
                // Free event - registration completed immediately
                result.put("message", "✅ Đăng ký sự kiện miễn phí thành công!\n\n" +
                        "🎉 Bạn đã được đăng ký tham gia sự kiện này.\n" +
                        "📧 Thông tin chi tiết sẽ được gửi qua email.");
            } else {
                // Paid event - need payment
                result.put("paymentUrl", payment.getCheckoutUrl());
                result.put("qrCode", payment.getQrCode());
                result.put("message", "✅ Đã tạo đơn hàng thành công!\n" +
                        "🔗 Link thanh toán: " + payment.getCheckoutUrl() + "\n\n" +
                        "💡 Vui lòng thanh toán để hoàn tất đăng ký.");
            }

            log.info("✅ DEBUG: Order creation completed successfully - orderId={}, userId={}, paymentId={}",
                    order.getOrderId(), userId, payment.getPaymentId());

            try {
                agentEventService.createOrUpdateEmailReminder(order.getEvent().getId(), 5, userId);
                log.info("✅ Đã tạo lịch nhắc nhở mặc định cho host khi tạo event ID: {}", order.getEvent().getId());
            } catch (Exception e) {
                log.error("❌ Lỗi khi tạo lịch nhắc nhở cho event ID: {} - {}",order.getEvent().getId(), e.getMessage(), e);
                // Không throw exception để không ảnh hưởng đến việc tạo event
            }

            return result;


        } catch (Exception e) {
            log.error("❌ DEBUG: Order creation failed with exception: {}", e.getMessage(), e);
            log.error("❌ DEBUG: Exception stack trace:", e);
            result.put("success", false);
            result.put("message", "❌ Lỗi khi tạo đơn hàng: " + e.getMessage());
            return result;
        }
    }

    /**
     * Cancel pending order
     */
    public String cancelOrder(Long userId) {
        if (pendingOrders.remove(userId) != null) {
            return "❌ Đã hủy đơn hàng.";
        }
        return "ℹ️ Không có đơn hàng nào đang chờ xử lý.";
    }

    /**
     * Get pending order status
     */
    public PendingOrder getPendingOrder(Long userId) {
        return pendingOrders.get(userId);
    }

    /**
     * Check if user has pending order
     */
    public boolean hasPendingOrder(Long userId) {
        return pendingOrders.containsKey(userId);
    }
}

