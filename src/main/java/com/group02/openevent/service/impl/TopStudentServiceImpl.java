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
            System.out.println("========================================");
            System.out.println("=== DEBUG: Getting top students by points (Native SQL) ===");
            System.out.println("Limit requested: " + limit);
            
            // Sử dụng native SQL query - đơn giản và trực tiếp
            List<Object[]> results = customerRepo.findTopStudentsByPointsNative();
            System.out.println("DEBUG: Native SQL query returned " + (results != null ? results.size() : 0) + " results");
            
            List<TopStudentDTO> topStudents = new ArrayList<>();
            
            if (results != null && !results.isEmpty()) {
                int rank = 1;
                for (Object[] row : results) {
                    if (topStudents.size() >= limit) {
                        break;
                    }
                    
                    // Parse kết quả từ native query
                    // Object[] format: [customer_id, name, points, email, image_url]
                    try {
                        Long customerId = ((Number) row[0]).longValue();
                        String name = row[1] != null ? row[1].toString() : null;
                        Integer points = row[2] != null ? ((Number) row[2]).intValue() : 0;
                        String email = row[3] != null ? row[3].toString() : "";
                        String imageUrl = row[4] != null ? row[4].toString() : null;
                        
                        System.out.println("DEBUG: Processing row - ID: " + customerId + 
                            ", Name: '" + name + "'" + 
                            ", Points: " + points);
                        
                        // Skip nếu không có name
                        if (name == null || name.trim().isEmpty() || name.equals("Chưa có dữ liệu")) {
                            System.out.println("DEBUG: Skipping - invalid name");
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
                        
                        System.out.println("DEBUG: ✓ Added student - Name: '" + dto.getName() + 
                            "', Points: " + dto.getPoints() + 
                            ", Rank: " + dto.getRank());
                        topStudents.add(dto);
                        
                    } catch (Exception e) {
                        System.out.println("DEBUG: Error parsing row: " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            } else {
                System.out.println("WARNING: Native SQL query returned 0 results!");
            }
            
            System.out.println("DEBUG: Total students added: " + topStudents.size());
            
            // Ensure we have exactly the requested number of students (fill with placeholder if needed)
            while (topStudents.size() < limit) {
                System.out.println("DEBUG: Adding placeholder student for rank " + (topStudents.size() + 1));
                topStudents.add(createPlaceholderStudent(topStudents.size() + 1));
            }
            
            System.out.println("DEBUG: Final result - Returning " + topStudents.size() + " students");
            for (int i = 0; i < topStudents.size(); i++) {
                TopStudentDTO s = topStudents.get(i);
                System.out.println("  Final[" + i + "]: Name='" + s.getName() + "', Points=" + s.getPoints() + ", Rank=" + s.getRank());
            }
            System.out.println("========================================");
            
            return topStudents;
        } catch (Exception e) {
            System.err.println("========================================");
            System.err.println("ERROR getting top students: " + e.getMessage());
            System.err.println("Exception type: " + e.getClass().getName());
            e.printStackTrace();
            System.err.println("========================================");
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

