package com.group02.openevent.service.impl;

import com.group02.openevent.dto.attendance.AttendanceRequest;
import com.group02.openevent.dto.attendance.AttendanceStatsDTO;
import com.group02.openevent.model.attendance.EventAttendance;
import com.group02.openevent.model.enums.Role;
import com.group02.openevent.model.event.Event;
import com.group02.openevent.model.order.Order;
import com.group02.openevent.model.ticket.TicketType;
import com.group02.openevent.model.account.Account;
import com.group02.openevent.model.user.Customer;
import com.group02.openevent.repository.IEventAttendanceRepo;
import com.group02.openevent.repository.IEventRepo;
import com.group02.openevent.repository.IOrderRepo;
import com.group02.openevent.repository.ITicketTypeRepo;
import com.group02.openevent.repository.ICustomerRepo;
import com.group02.openevent.repository.IAccountRepo;
import com.group02.openevent.service.EventAttendanceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class EventAttendanceServiceImpl implements EventAttendanceService {
    
    @Autowired
    private IEventAttendanceRepo attendanceRepo;
    
    @Autowired
    private IEventRepo eventRepo;

    @Autowired
    private IOrderRepo orderRepo;

    @Autowired
    private IOrderRepo orderRepository;

    @Autowired
    private ITicketTypeRepo ticketTypeRepository;

    @Autowired
    private ICustomerRepo customerRepo;

    @Autowired
    private IAccountRepo accountRepo;

    @Override
    @Transactional
    public EventAttendance checkIn(Long eventId, AttendanceRequest request) {
        log.info("Processing check-in for event {} with email {}", eventId, request.getEmail());
        
        // Validate event exists
        Event event = eventRepo.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found with ID: " + eventId));

        String normalizedEmail = request.getEmail() != null ? request.getEmail().trim().toLowerCase() : null;

        // Enforce ticket ownership (must have PAID order for this event)
        boolean hasPaidOrder = normalizedEmail != null && orderRepo.existsPaidByEventIdAndParticipantEmail(eventId, normalizedEmail);
        if (!hasPaidOrder) {
            throw new RuntimeException("Bạn không đăng ký sự kiện này (không tìm thấy vé đã thanh toán).");
        }
        
        // TÌM EventAttendance đã tồn tại (đã được tạo khi order được thanh toán)
        Optional<EventAttendance> existingOpt = attendanceRepo.findByEventIdAndEmail(eventId, normalizedEmail);
        
        if (existingOpt.isEmpty()) {
            // EventAttendance phải đã được tạo khi order được thanh toán
            // Nếu không tìm thấy, có thể order chưa được thanh toán hoặc có lỗi trong quá trình tạo
            throw new RuntimeException("Không tìm thấy thông tin đăng ký. Vui lòng kiểm tra lại email hoặc liên hệ ban tổ chức.");
        }
        
        EventAttendance existing = existingOpt.get();
        
        // Kiểm tra đã check-in chưa
        if (existing.getCheckInTime() != null) {
            throw new RuntimeException("Email này đã check-in lúc " + existing.getCheckInTime());
        }
        
        if (existing.getStatus() == EventAttendance.AttendanceStatus.CHECKED_IN 
            || existing.getStatus() == EventAttendance.AttendanceStatus.CHECKED_OUT) {
            throw new RuntimeException("Email này đã check-in rồi");
        }
        
        // CẬP NHẬT EventAttendance đã tồn tại (UPDATE, không tạo mới)
        existing.setCheckInTime(LocalDateTime.now());
        existing.setStatus(EventAttendance.AttendanceStatus.CHECKED_IN);
        
        // Cập nhật thông tin nếu có thay đổi (optional)
        if (request.getFullName() != null && !request.getFullName().trim().isEmpty()) {
            existing.setFullName(request.getFullName());
        }
        if (request.getPhone() != null && !request.getPhone().trim().isEmpty()) {
            existing.setPhone(request.getPhone());
        }
        if (request.getOrganization() != null && !request.getOrganization().trim().isEmpty()) {
            existing.setOrganization(request.getOrganization());
        }
        
        log.info("Updated EventAttendance {} for check-in at {}", existing.getAttendanceId(), existing.getCheckInTime());
        return attendanceRepo.save(existing);
    }
    
    @Override
    @Transactional
    public EventAttendance checkOut(Long eventId, String email) {
        String normalizedEmail = email != null ? email.trim().toLowerCase() : null;
        log.info("Processing check-out for event {} with email {}", eventId, normalizedEmail);
        
        // Find attendance record
        EventAttendance attendance = attendanceRepo.findByEventIdAndEmail(eventId, normalizedEmail)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin check-in với email: " + email));
        
        // Verify ticket ownership for checkout (must have PAID order)
        boolean hasPaidOrder = normalizedEmail != null && orderRepo.existsPaidByEventIdAndParticipantEmail(eventId, normalizedEmail);
        if (!hasPaidOrder) {
            throw new RuntimeException("Bạn không đăng ký sự kiện này (không tìm thấy vé đã thanh toán).");
        }

        // Validate status
        if (attendance.getStatus() != EventAttendance.AttendanceStatus.CHECKED_IN) {
            throw new RuntimeException("Bạn chưa check-in hoặc đã check-out rồi");
        }
        
        if (attendance.getCheckOutTime() != null) {
            throw new RuntimeException("Bạn đã check-out lúc " + attendance.getCheckOutTime());
        }
        
        // Update check-out
        attendance.setCheckOutTime(LocalDateTime.now());
        attendance.setStatus(EventAttendance.AttendanceStatus.CHECKED_OUT);
        
        return attendanceRepo.save(attendance);
    }
    
    @Override
    public List<EventAttendance> getAttendancesByEventId(Long eventId) {
        return attendanceRepo.findByEventId(eventId);
    }
    
    @Override
    public Optional<EventAttendance> getAttendanceByEventAndEmail(Long eventId, String email) {
        return attendanceRepo.findByEventIdAndEmail(eventId, email);
    }
    
    @Override
    public AttendanceStatsDTO getAttendanceStats(Long eventId) {
        Event event = eventRepo.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found with ID: " + eventId));
        
        long totalCheckedIn = attendanceRepo.countCheckedInByEventId(eventId);
        long totalCheckedOut = attendanceRepo.countCheckedOutByEventId(eventId);
        long currentlyPresent = attendanceRepo.countCurrentlyPresentByEventId(eventId);
        long totalAttendees = attendanceRepo.countByEventId(eventId);
        
        return new AttendanceStatsDTO(
                eventId,
                event.getTitle(),
                totalCheckedIn,
                totalCheckedOut,
                currentlyPresent,
                totalAttendees
        );
    }
    
    @Override
    public boolean isAlreadyCheckedIn(Long eventId, String email) {
        return attendanceRepo.existsByEventIdAndEmailAndCheckedIn(eventId, email);
    }
    public Page<EventAttendance> getAttendeesByEvent(Long eventId, Pageable pageable) {
        return attendanceRepo.findByEvent_Id(eventId, pageable);
    }
    public Page<EventAttendance> searchAttendees(Long eventId, String search, Pageable pageable) {
        return attendanceRepo.searchAttendees(eventId, search, pageable);
    }
    public Page<EventAttendance> filterAttendees(
            Long eventId,
            Long ticketTypeFilter,
            String paymentStatusFilter,
            String checkinStatusFilter,
            Pageable pageable) {

        return attendanceRepo.filterAttendees(
                eventId, ticketTypeFilter, paymentStatusFilter, checkinStatusFilter, pageable);
    }

    public List<EventAttendance> filterAttendees(
            Long eventId,
            Long ticketTypeFilter,
            String paymentStatusFilter,
            String checkinStatusFilter) {

        return attendanceRepo.filterAttendees(
                eventId, ticketTypeFilter, paymentStatusFilter, checkinStatusFilter);
    }

    public EventAttendance listCheckIn(Long id,Long attendanceId) {
        // TÌM EventAttendance đã tồn tại (đã được tạo khi order được thanh toán)
        EventAttendance attendance = attendanceRepo.findByEvent_IdAndAttendanceId(id,attendanceId)
                .orElseThrow(() -> new RuntimeException("Người tham dự không tìm thấy"));

        // Kiểm tra đã check-in chưa
        if (attendance.getCheckInTime() != null) {
            throw new RuntimeException("Người này đã check-in rồi");
        }

        // CẬP NHẬT EventAttendance đã tồn tại (UPDATE, không tạo mới)
        attendance.setCheckInTime(LocalDateTime.now());
        attendance.setStatus(EventAttendance.AttendanceStatus.CHECKED_IN);
        
        log.info("Updated EventAttendance {} for check-in at {}", attendance.getAttendanceId(), attendance.getCheckInTime());
        return attendanceRepo.save(attendance);
    }

    public EventAttendance checkOut(Long id,Long attendanceId) {
        EventAttendance attendance = attendanceRepo.findByEvent_IdAndAttendanceId(id,attendanceId)
                .orElseThrow(() -> new RuntimeException("Người tham dự không tìm thấy"));

        if (attendance.getCheckInTime() == null) {
            throw new RuntimeException("Phải check-in trước khi check-out");
        }

        if (attendance.getCheckOutTime() != null) {
            throw new RuntimeException("Người này đã check-out rồi");
        }

        attendance.setCheckOutTime(LocalDateTime.now());
        return attendanceRepo.save(attendance);
    }

    // NOTE: getOrCreateSystemCustomer() đã được xóa vì không còn tạo Order
    // Attendees thêm thủ công không có Order để không làm sai số liệu thống kê

    public EventAttendance addAttendee(Long eventId, String name, String email,
                                       String phone, Long ticketTypeId, String organization) {
        
        // Validate required fields
        if (name == null || name.trim().isEmpty()) {
            throw new RuntimeException("Tên người tham dự không được để trống");
        }
        if (email == null || email.trim().isEmpty()) {
            throw new RuntimeException("Email không được để trống");
        }
        if (phone == null || phone.trim().isEmpty()) {
            throw new RuntimeException("Số điện thoại không được để trống");
        }
        
        // Trim và normalize values
        name = name.trim();
        email = email.trim().toLowerCase();
        phone = phone.trim();
        organization = (organization != null) ? organization.trim() : null;

        // Validate event exists
        Event event = eventRepo.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Sự kiện không tìm thấy"));
        
        // Validate ticket type exists (chỉ để validate, không tạo Order)
        TicketType ticketType = ticketTypeRepository.findById(ticketTypeId)
                .orElseThrow(() -> new RuntimeException("Loại vé không tìm thấy"));
        if (!ticketType.getEvent().getId().equals(eventId)) {
            throw new RuntimeException("Loại vé không thuộc sự kiện này");
        }

        // Tạo EventAttendance KHÔNG có Order (không tính vào thống kê mua vé)
        // Đây là người được thêm thủ công bởi ban tổ chức, không phải khách mua vé
        EventAttendance attendance = new EventAttendance();
        attendance.setOrder(null); // Quan trọng: null để không tính vào thống kê
        attendance.setEvent(event);
        attendance.setNotes("Thêm bởi ban tổ chức - Loại vé: " + ticketType.getName());

        attendance.setFullName(name);
        attendance.setEmail(email);
        attendance.setPhone(phone);
        attendance.setStatus(EventAttendance.AttendanceStatus.PENDING);
        attendance.setOrganization(organization);

        return attendanceRepo.save(attendance);
    }

    public EventAttendance updateAttendee(Long id,Long attendanceId, String name, String email,
                                          String phone, String organization) {
        // Validate required fields
        if (name == null || name.trim().isEmpty()) {
            throw new RuntimeException("Tên người tham dự không được để trống");
        }
        if (email == null || email.trim().isEmpty()) {
            throw new RuntimeException("Email không được để trống");
        }
        if (phone == null || phone.trim().isEmpty()) {
            throw new RuntimeException("Số điện thoại không được để trống");
        }
        
        // Trim và normalize values
        name = name.trim();
        email = email.trim().toLowerCase();
        phone = phone.trim();
        organization = (organization != null) ? organization.trim() : null;
        
        EventAttendance attendance = attendanceRepo.findByEvent_IdAndAttendanceId(id,attendanceId)
                .orElseThrow(() -> new RuntimeException("Người tham dự không tìm thấy"));

        // Update EventAttendance
        attendance.setFullName(name);
        attendance.setEmail(email);
        attendance.setPhone(phone);
        attendance.setOrganization(organization);
        
        // KHÔNG update Order - vì attendee thủ công không có Order
        // Nếu attendee có Order (người mua vé thật), không nên edit từ đây

        return attendanceRepo.save(attendance);
    }

    public void deleteAttendee(Long eventId, Long attendanceId) {
        // 1. Tìm người tham dự PHẢI thuộc sự kiện này
        EventAttendance attendance = attendanceRepo.findByEvent_IdAndAttendanceId(eventId, attendanceId)
                .orElseThrow(() -> new RuntimeException("Người tham dự không tìm thấy hoặc không thuộc sự kiện này"));

        // 2. Logic xóa (giữ nguyên)
        attendanceRepo.delete(attendance);
        if (attendance.getOrder() != null) {
            orderRepository.delete(attendance.getOrder());
        }
    }

    /**
     * Tạo EventAttendance từ Order khi order được thanh toán thành công
     */
    @Override
    @Transactional
    public EventAttendance createAttendanceFromOrder(Order order) {
        log.info("Creating EventAttendance from Order: {}", order.getOrderId());
        
        // Validate order
        if (order == null) {
            throw new RuntimeException("Order cannot be null");
        }
        
        if (order.getEvent() == null) {
            throw new RuntimeException("Order must have an associated event");
        }
        
        // Check if EventAttendance already exists for this order
        if (order.getOrderId() != null) {
            Optional<EventAttendance> existing = attendanceRepo.findByOrder_OrderId(order.getOrderId());
            if (existing.isPresent()) {
                log.info("EventAttendance already exists for order: {}, skipping creation", order.getOrderId());
                return existing.get();
            }
        }
        
        // Validate order status (should be PAID)
        if (order.getStatus() != com.group02.openevent.model.order.OrderStatus.PAID) {
            log.warn("Order {} is not PAID (status: {}), skipping EventAttendance creation", 
                order.getOrderId(), order.getStatus());
            throw new RuntimeException("Cannot create EventAttendance for non-PAID order");
        }
        
        // Get participant info from order
        String participantName = order.getParticipantName();
        String participantEmail = order.getParticipantEmail();
        String participantPhone = order.getParticipantPhone();
        String participantOrganization = order.getParticipantOrganization();
        
        // Validate required fields
        if (participantName == null || participantName.trim().isEmpty()) {
            throw new RuntimeException("Order must have participant name");
        }
        if (participantEmail == null || participantEmail.trim().isEmpty()) {
            throw new RuntimeException("Order must have participant email");
        }
        
        // Normalize email
        String normalizedEmail = participantEmail.trim().toLowerCase();
        
        // Check if attendance already exists for this event and email
        Optional<EventAttendance> existingByEmail = attendanceRepo.findByEventIdAndEmail(
            order.getEvent().getId(), normalizedEmail);
        
        if (existingByEmail.isPresent()) {
            EventAttendance existing = existingByEmail.get();
            // Update existing attendance to link with order if not already linked
            if (existing.getOrder() == null && order.getOrderId() != null) {
                existing.setOrder(order);
                existing.setFullName(participantName);
                existing.setEmail(normalizedEmail);
                existing.setPhone(participantPhone);
                existing.setOrganization(participantOrganization);
                log.info("Updated existing EventAttendance {} to link with order {}", 
                    existing.getAttendanceId(), order.getOrderId());
                return attendanceRepo.save(existing);
            }
            log.info("EventAttendance already exists for event {} and email {}, skipping creation", 
                order.getEvent().getId(), normalizedEmail);
            return existing;
        }
        
        // Create new EventAttendance
        EventAttendance attendance = new EventAttendance();
        attendance.setOrder(order);
        attendance.setEvent(order.getEvent());
        attendance.setCustomer(order.getCustomer());
        
        // Set participant info from order
        attendance.setFullName(participantName);
        attendance.setEmail(normalizedEmail);
        attendance.setPhone(participantPhone != null ? participantPhone.trim() : null);
        attendance.setOrganization(participantOrganization != null ? participantOrganization.trim() : null);
        
        // Set initial status
        attendance.setStatus(EventAttendance.AttendanceStatus.PENDING);
        attendance.setNotes("Tạo tự động từ đơn hàng #" + order.getOrderId());
        
        EventAttendance saved = attendanceRepo.save(attendance);
        log.info("Created EventAttendance {} for order {}", saved.getAttendanceId(), order.getOrderId());
        
        return saved;
    }

}



