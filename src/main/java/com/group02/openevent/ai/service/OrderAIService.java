package com.group02.openevent.ai.service;

import com.group02.openevent.ai.dto.PendingOrder;
import com.group02.openevent.dto.order.CreateOrderWithTicketTypeRequest;
import com.group02.openevent.model.event.Event;
import com.group02.openevent.model.order.Order;
import com.group02.openevent.model.payment.Payment;
import com.group02.openevent.model.ticket.TicketType;
import com.group02.openevent.model.user.Customer;
import com.group02.openevent.repository.IUserRepo;
import com.group02.openevent.service.EventService;
import com.group02.openevent.service.OrderService;
import com.group02.openevent.service.PaymentService;
import com.group02.openevent.service.TicketTypeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
    private final IUserRepo userRepo;
    private final AgentEventService  agentEventService;

    // Store pending orders by userId
    private final Map<Long, PendingOrder> pendingOrders = new HashMap<>();

    /**
     * Start order creation process
     */
    public String startOrderCreation(Long userId, String eventQuery) {
        // Search for event (PUBLIC status only for ticket buying)
        List<Event> events = eventService.findByTitleAndPublicStatus(eventQuery);

        if (events.isEmpty()) {
            return "❌ Không tìm thấy sự kiện \"" + eventQuery + "\". Vui lòng kiểm tra lại tên sự kiện.";
        }

        Event event = events.get(0);

        // Create pending order
        PendingOrder pendingOrder = new PendingOrder();
        pendingOrder.setEvent(event);
        pendingOrder.setCurrentStep(PendingOrder.OrderStep.SELECT_TICKET_TYPE);
        pendingOrders.put(userId, pendingOrder);

        // Get available ticket types
        List<TicketType> ticketTypes = ticketTypeService.getTicketTypesByEventId(event.getId());

        if (ticketTypes.isEmpty()) {
            return "❌ Sự kiện \"" + event.getTitle() + "\" hiện không có vé nào.";
        }

        StringBuilder response = new StringBuilder();
        response.append("🎫 Sự kiện: **").append(event.getTitle()).append("**\n\n");
        response.append("📅 Thời gian: ").append(event.getStartsAt()).append("\n\n");
        response.append("Các loại vé có sẵn:\n\n");

        for (TicketType ticket : ticketTypes) {
            response.append("• **").append(ticket.getName()).append("**\n");
            response.append("  - Giá: ").append(ticket.getFinalPrice()).append(" VND\n");
            response.append("  - Còn lại: ").append(ticket.getAvailableQuantity()).append(" vé\n");
            if (ticket.getDescription() != null) {
                response.append("  - Mô tả: ").append(ticket.getDescription()).append("\n");
            }
            response.append("\n");
        }

        response.append("💡 Bạn muốn chọn loại vé nào?");

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
            Optional<Customer> customerOpt = userRepo.findByAccount_AccountId(userId);
            if (customerOpt.isEmpty()) {
                log.error("❌ DEBUG: Customer not found for userId: {}", userId);
                result.put("success", false);
                result.put("message", "❌ Không tìm thấy thông tin khách hàng.");
                return result;
            }

            Customer customer = customerOpt.get();
            log.info("🔍 DEBUG: Customer found - customerId: {}, email: {}",
                    customer.getCustomerId(), customer.getAccount().getEmail());

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

            // Create payment link
            log.info("🔍 DEBUG: Creating payment link...");
            String returnUrl = "http://localhost:8080/payment/success?orderId=" + order.getOrderId();
            String cancelUrl = "http://localhost:8080/payment/cancel?orderId=" + order.getOrderId();
            Payment payment = paymentService.createPaymentLinkForOrder(order, returnUrl, cancelUrl);
            log.info("🔍 DEBUG: Payment created successfully - paymentId: {}, checkoutUrl: {}",
                    payment.getPaymentId(), payment.getCheckoutUrl());

            // Clear pending order
            pendingOrders.remove(userId);
            log.info("🔍 DEBUG: Pending order cleared for userId: {}", userId);

            // Return success
            result.put("success", true);
            result.put("orderId", order.getOrderId());
            result.put("paymentUrl", payment.getCheckoutUrl());
            result.put("qrCode", payment.getQrCode());
            result.put("amount", payment.getAmount());
            result.put("message", "✅ Đã tạo đơn hàng thành công!\n" +
                    "🔗 Link thanh toán: " + payment.getCheckoutUrl() + "\n\n" +
                    "💡 Vui lòng thanh toán để hoàn tất đăng ký.");

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

