package com.group02.openevent.service.impl;

import com.group02.openevent.dto.face.FaceVerificationResult;
import com.group02.openevent.model.attendance.EventAttendance;
import com.group02.openevent.model.order.Order;
import com.group02.openevent.model.order.OrderStatus;
import com.group02.openevent.model.user.Customer;
import com.group02.openevent.model.user.User;
import com.group02.openevent.repository.IEventAttendanceRepo;
import com.group02.openevent.repository.IOrderRepo;
import com.group02.openevent.service.FaceCheckinService;
import com.group02.openevent.service.FaceVerificationClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Implementation of face-based check-in service
 */
@Service
@Slf4j
public class FaceCheckinServiceImpl implements FaceCheckinService {
    
    @Autowired
    private FaceVerificationClient faceVerificationClient;
    
    @Autowired
    private IOrderRepo orderRepo;
    
    @Autowired
    private IEventAttendanceRepo attendanceRepo;
    
    @Override
    @Transactional
    public EventAttendance faceCheckIn(Long eventId, byte[] capturedImage, Customer currentCustomer) {
        log.info("Processing face check-in for event {} and customer {}", eventId, currentCustomer.getCustomerId());
        
        // 1. Validate customer has avatarUrl (ảnh profile đã được lưu)
        String avatarUrl = currentCustomer.getAvatarUrl();
        if (avatarUrl == null || avatarUrl.trim().isEmpty()) {
            throw new RuntimeException("Bạn chưa có ảnh đại diện. Vui lòng cập nhật ảnh đại diện trong hồ sơ trước khi sử dụng check-in bằng khuôn mặt.");
        }
        
        // 2. Validate customer has registered face
        Boolean faceRegistered = currentCustomer.getFaceRegistered();
        if (faceRegistered == null || !faceRegistered) {
            throw new RuntimeException("Bạn chưa đăng ký khuôn mặt. Vui lòng đăng ký khuôn mặt trong hồ sơ trước khi sử dụng check-in bằng khuôn mặt.");
        }
        
        // 3. Verify face match (so sánh ảnh check-in với ảnh profile)
        // Note: Ảnh check-in (capturedImage) chỉ dùng để verify, không lưu
        FaceVerificationResult verificationResult = faceVerificationClient.verifyFace(currentCustomer, capturedImage);
        
        if (!verificationResult.isMatch()) {
            log.warn("Face verification failed for customer {}: {}", 
                currentCustomer.getCustomerId(), verificationResult.getMessage());
            throw new RuntimeException("Khuôn mặt không khớp. Vui lòng thử lại hoặc sử dụng phương thức check-in khác.");
        }
        
        log.info("Face verification successful for customer {} with confidence: {}", 
            currentCustomer.getCustomerId(), verificationResult.getConfidence());
        
        // ✅ 4. Kiểm tra order đã thanh toán bằng customerId
        boolean hasPaidOrder = orderRepo.existsPaidByEventIdAndCustomerId(eventId, currentCustomer.getCustomerId());
        if (!hasPaidOrder) {
            throw new RuntimeException("Bạn không đăng ký sự kiện này (không tìm thấy vé đã thanh toán).");
        }
        
        // ✅ 5. Tìm paid order cho event này
        List<Order> customerOrders = orderRepo.findByCustomerId(currentCustomer.getCustomerId());
        List<Order> paidOrdersForEvent = customerOrders.stream()
            .filter(order -> order.getEvent() != null && 
                           order.getEvent().getId().equals(eventId) &&
                           order.getStatus() == OrderStatus.PAID)
            .collect(Collectors.toList());
        
        if (paidOrdersForEvent.isEmpty()) {
            throw new RuntimeException("Bạn không đăng ký sự kiện này (không tìm thấy vé đã thanh toán).");
        }
        
        // ✅ 6. Tìm EventAttendance bằng orderId (tránh vấn đề normalize email)
        Order paidOrder = paidOrdersForEvent.get(0);
        Optional<EventAttendance> attendanceOpt = attendanceRepo.findByOrder_OrderId(paidOrder.getOrderId());
        
        if (attendanceOpt.isEmpty()) {
            throw new RuntimeException("Không tìm thấy thông tin đăng ký. Vui lòng kiểm tra lại hoặc liên hệ ban tổ chức.");
        }
        
        EventAttendance attendance = attendanceOpt.get();
        
        // Kiểm tra đã check-in chưa
        if (attendance.getCheckInTime() != null) {
            throw new RuntimeException("Bạn đã check-in lúc " + attendance.getCheckInTime());
        }
        
        if (attendance.getStatus() == EventAttendance.AttendanceStatus.CHECKED_IN 
            || attendance.getStatus() == EventAttendance.AttendanceStatus.CHECKED_OUT) {
            throw new RuntimeException("Bạn đã check-in rồi");
        }
        
        // ✅ 7. Cập nhật EventAttendance trực tiếp (không cần kiểm tra email lại)
        attendance.setCheckInTime(LocalDateTime.now());
        attendance.setStatus(EventAttendance.AttendanceStatus.CHECKED_IN);
        
        // Cập nhật thông tin từ user nếu có thay đổi
        User user = currentCustomer.getUser();
        if (user != null) {
            if (user.getName() != null && !user.getName().trim().isEmpty()) {
                attendance.setFullName(user.getName());
            }
            if (user.getPhoneNumber() != null && !user.getPhoneNumber().trim().isEmpty()) {
                attendance.setPhone(user.getPhoneNumber());
            }
        }
        
        if (currentCustomer.getOrganization() != null && currentCustomer.getOrganization().getOrgName() != null) {
            attendance.setOrganization(currentCustomer.getOrganization().getOrgName());
        }
        
        EventAttendance saved = attendanceRepo.save(attendance);
        
        log.info("Face check-in successful for event {} and customer {} with orderId {}", 
            eventId, currentCustomer.getCustomerId(), paidOrder.getOrderId());
        
        return saved;
    }
}

