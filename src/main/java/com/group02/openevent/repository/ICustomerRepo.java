package com.group02.openevent.repository;

import com.group02.openevent.model.user.Customer;
import com.group02.openevent.model.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ICustomerRepo extends JpaRepository<Customer, Long> {
    Optional<Customer> findByUser(User user);
    Optional<Customer> findByUser_UserId(Long userId);
    Optional<Customer> findByUser_Account_AccountId(Long accountId);
    Optional<Customer> findByUser_Account_Email(String email);
    @Query(value = "SELECT c.customer_id, u.name, c.points, a.email, u.avatar " +
            "FROM customer c " +
            "INNER JOIN `user` u ON c.user_id = u.user_id " +
            "INNER JOIN account a ON u.account_id = a.account_id " +
            "WHERE u.name IS NOT NULL AND u.name != '' AND u.name != 'Chưa có dữ liệu' " +
            "ORDER BY c.points DESC", nativeQuery = true)
    List<Object[]> findTopStudentsByPointsNative();
}