package com.group02.openevent.repository;

import com.group02.openevent.model.volunteer.VolunteerApplication;
import com.group02.openevent.model.volunteer.VolunteerStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IVolunteerApplicationRepo extends JpaRepository<VolunteerApplication, Long> {

    /**
     * Tìm tất cả volunteer applications cho một event
     */
    List<VolunteerApplication> findByEvent_Id(Long eventId);

    /**
     * Tìm tất cả volunteer applications cho một event với status cụ thể
     */
    List<VolunteerApplication> findByEvent_IdAndStatus(Long eventId, VolunteerStatus status);

    /**
     * Tìm volunteer application của một customer cho một event
     */
    Optional<VolunteerApplication> findByCustomer_CustomerIdAndEvent_Id(Long customerId, Long eventId);

    /**
     * Kiểm tra xem customer đã apply làm volunteer cho event này chưa
     */
    boolean existsByCustomer_CustomerIdAndEvent_Id(Long customerId, Long eventId);

    /**
     * Tìm tất cả volunteer applications của một customer
     */
    List<VolunteerApplication> findByCustomer_CustomerId(Long customerId);

    /**
     * Tìm tất cả volunteer applications của một customer với status cụ thể
     */
    List<VolunteerApplication> findByCustomer_CustomerIdAndStatus(Long customerId, VolunteerStatus status);

    /**
     * Đếm số lượng volunteer applications approved cho một event
     */
    long countByEvent_IdAndStatus(Long eventId, VolunteerStatus status);

    /**
     * Đếm số lượng volunteer applications pending cho một event
     */
    @Query("SELECT COUNT(v) FROM VolunteerApplication v WHERE v.event.id = :eventId AND v.status = 'PENDING'")
    long countPendingApplicationsByEventId(@Param("eventId") Long eventId);
}

