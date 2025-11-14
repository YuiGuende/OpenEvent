package com.group02.openevent.service.impl;

import com.group02.openevent.dto.attendance.AttendanceRequest;
import com.group02.openevent.dto.face.FaceVerificationResult;
import com.group02.openevent.model.attendance.EventAttendance;
import com.group02.openevent.model.user.Customer;
import com.group02.openevent.model.user.User;
import com.group02.openevent.service.EventAttendanceService;
import com.group02.openevent.service.FaceCheckinService;
import com.group02.openevent.service.FaceVerificationClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of face-based check-in service
 */
@Service
@Slf4j
public class FaceCheckinServiceImpl implements FaceCheckinService {
    
    @Autowired
    private FaceVerificationClient faceVerificationClient;
    
    @Autowired
    private EventAttendanceService attendanceService;
    
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
        
        // 4. Build AttendanceRequest from customer profile
        User user = currentCustomer.getUser();
        AttendanceRequest request = new AttendanceRequest();
        
        if (user != null) {
            request.setFullName(user.getName());
            request.setPhone(user.getPhoneNumber());
            
            if (user.getAccount() != null) {
                request.setEmail(user.getAccount().getEmail());
            }
        }
        
        if (currentCustomer.getOrganization() != null && currentCustomer.getOrganization().getOrgName() != null) {
            request.setOrganization(currentCustomer.getOrganization().getOrgName());
        }
        
        // 5. Call existing check-in service (reuse all existing logic)
        EventAttendance attendance = attendanceService.checkIn(eventId, request);
        
        log.info("Face check-in successful for event {} and customer {}", eventId, currentCustomer.getCustomerId());
        
        return attendance;
    }
}

