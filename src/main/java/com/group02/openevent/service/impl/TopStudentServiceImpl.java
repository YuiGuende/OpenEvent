package com.group02.openevent.service.impl;

import com.group02.openevent.dto.home.TopStudentDTO;
import com.group02.openevent.repository.ICustomerRepo;
import com.group02.openevent.service.TopStudentService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional(readOnly = true, noRollbackFor = Exception.class)
public class TopStudentServiceImpl implements TopStudentService {

    private final ICustomerRepo customerRepo;

    public TopStudentServiceImpl(ICustomerRepo customerRepo) {
        this.customerRepo = customerRepo;
    }

    @Override
    public List<TopStudentDTO> getTopStudents(int limit) {
        try {
            // Sử dụng native SQL query - đơn giản và trực tiếp
            List<Object[]> results = customerRepo.findTopStudentsByPointsNative();
            
            List<TopStudentDTO> topStudents = new ArrayList<>();
            
            if (results != null && !results.isEmpty()) {
                int rank = 1;
                for (Object[] row : results) {
                    if (topStudents.size() >= limit) {
                        break;
                    }
                    
                    // Parse kết quả từ native query
                    // Object[] format: [customer_id, name, points, email, avatar]
                    // name từ user.name, email từ account.email, avatar từ user.avatar
                    try {
                        Long customerId = ((Number) row[0]).longValue();
                        String name = row[1] != null ? row[1].toString() : null;
                        Integer points = row[2] != null ? ((Number) row[2]).intValue() : 0;
                        String email = row[3] != null ? row[3].toString() : "";
                        String imageUrl = row[4] != null ? row[4].toString() : null;
                        
                        // Skip nếu không có name
                        if (name == null || name.trim().isEmpty() || name.equals("Chưa có dữ liệu")) {
                            continue;
                        }
                        
                        // Tạo DTO từ dữ liệu native query
                        TopStudentDTO dto = TopStudentDTO.builder()
                                .customerId(customerId)
                                .name(name)
                                .email(email != null ? email : "")
                                .imageUrl(imageUrl != null && !imageUrl.trim().isEmpty() ? imageUrl : getDefaultImageUrl(customerId))
                                .organization(null) // Không có trong query
                                .points(points)
                                .rank(rank++)
                                .build();
                        
                        topStudents.add(dto);
                        
                    } catch (Exception e) {
                        // Skip invalid rows silently
                    }
                }
            }
            
            // Ensure we have exactly the requested number of students (fill with placeholder if needed)
            while (topStudents.size() < limit) {
                topStudents.add(createPlaceholderStudent(topStudents.size() + 1));
            }
            
            return topStudents;
        } catch (Exception e) {
            // Return placeholder students on error
            List<TopStudentDTO> placeholders = new ArrayList<>();
            for (int i = 1; i <= limit; i++) {
                placeholders.add(createPlaceholderStudent(i));
            }
            return placeholders;
        }
    }
    
    /**
     * Get default image URL based on customer ID
     */
    private String getDefaultImageUrl(Long customerId) {
        if (customerId == null) {
            return "/img/sinhvien2.jpg";
        }
        
        // Use modulo to cycle through available images
        int imageIndex = (int) (customerId % 3);
        switch (imageIndex) {
            case 0: return "/img/sinhvien2.jpg";
            case 1: return "/img/sinhvien3.jpg";
            default: return "/img/sinhvien2.jpg";
        }
    }
    
    /**
     * Create placeholder student when not enough real data
     */
    private TopStudentDTO createPlaceholderStudent(int rank) {
        return TopStudentDTO.builder()
                .customerId(null)
                .name("Chưa có dữ liệu")
                .email("")
                .imageUrl("/img/sinhvien2.jpg")
                .organization(null)
                .points(0)
                .rank(rank)
                .build();
    }
}

