package com.group02.openevent.repository;

import com.group02.openevent.model.account.Account;
import com.group02.openevent.model.user.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ICustomerRepo extends JpaRepository<Customer, Long> {
    boolean existsByPhoneNumber(String phoneNumber);
    Optional<Customer> findByAccount(Account account);
    Optional<Customer> findByAccount_AccountId(Long accountId);
    
    /**
     * Get top students by points (sắp xếp theo points giảm dần)
     * Sử dụng native SQL query để tránh vấn đề với JPA/Hibernate
     * Lấy tất cả customers hợp lệ, giới hạn sẽ được thực hiện ở service layer
     */
    @Query(value = "SELECT customer_id, name, points, email, image_url " +
            "FROM customer " +
            "WHERE name IS NOT NULL AND name != '' AND name != 'Chưa có dữ liệu' " +
            "ORDER BY points DESC", nativeQuery = true)
    List<Object[]> findTopStudentsByPointsNative();
}

