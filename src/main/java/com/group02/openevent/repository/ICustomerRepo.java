package com.group02.openevent.repository;

import com.group02.openevent.model.account.Account;
import com.group02.openevent.model.user.Customer;
import com.group02.openevent.model.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.List;
import java.util.Optional;

public interface ICustomerRepo extends JpaRepository<Customer, Long> {
    Optional<Customer> findByUser(User user);
    Optional<Customer> findByUser_UserId(Long userId);
    Optional<Customer> findByUser_Account_AccountId(Long accountId);
    Optional<Customer> findByUser_Account_Email(String email);
    @Query(value = "SELECT customer_id, name, points, email, image_url " +
            "FROM customer " +
            "WHERE name IS NOT NULL AND name != '' AND name != 'Chưa có dữ liệu' " +
            "ORDER BY points DESC", nativeQuery = true)
    List<Object[]> findTopStudentsByPointsNative();
}