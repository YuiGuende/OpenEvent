package com.group02.openevent.service;

import com.group02.openevent.model.volunteer.VolunteerApplication;
import com.group02.openevent.model.volunteer.VolunteerStatus;

import java.util.List;
import java.util.Optional;

public interface VolunteerService {

    /**
     * Tạo volunteer application mới
     * @param customerId ID của customer
     * @param eventId ID của event
     * @param applicationMessage Lời nhắn từ volunteer (optional)
     * @return VolunteerApplication đã được tạo
     */
    VolunteerApplication createVolunteerApplication(Long customerId, Long eventId, String applicationMessage);

    /**
     * Host duyệt volunteer application (approve)
     * @param applicationId ID của volunteer application
     * @param reviewerAccountId ID của account host đang review
     * @param hostResponse Phản hồi từ host (optional)
     * @return VolunteerApplication đã được approve
     */
    VolunteerApplication approveVolunteerApplication(Long applicationId, Long reviewerAccountId, String hostResponse);

    /**
     * Host từ chối volunteer application (reject)
     * @param applicationId ID của volunteer application
     * @param reviewerAccountId ID của account host đang review
     * @param hostResponse Lý do từ chối (optional)
     * @return VolunteerApplication đã bị reject
     */
    VolunteerApplication rejectVolunteerApplication(Long applicationId, Long reviewerAccountId, String hostResponse);

    /**
     * Lấy volunteer application theo ID
     */
    Optional<VolunteerApplication> getVolunteerApplicationById(Long applicationId);

    /**
     * Lấy tất cả volunteer applications cho một event
     */
    List<VolunteerApplication> getVolunteerApplicationsByEventId(Long eventId);

    /**
     * Lấy tất cả volunteer applications cho một event với status cụ thể
     */
    List<VolunteerApplication> getVolunteerApplicationsByEventIdAndStatus(Long eventId, VolunteerStatus status);

    /**
     * Lấy volunteer application của một customer cho một event
     */
    Optional<VolunteerApplication> getVolunteerApplicationByCustomerAndEvent(Long customerId, Long eventId);

    /**
     * Kiểm tra xem customer đã apply làm volunteer cho event này chưa
     */
    boolean hasCustomerAppliedAsVolunteer(Long customerId, Long eventId);

    /**
     * Kiểm tra xem customer có được approved làm volunteer cho event này không
     */
    boolean isCustomerApprovedVolunteer(Long customerId, Long eventId);

    /**
     * Lấy tất cả volunteer applications của một customer
     */
    List<VolunteerApplication> getVolunteerApplicationsByCustomerId(Long customerId);

    /**
     * Lấy tất cả volunteer applications của một customer với status cụ thể
     */
    List<VolunteerApplication> getVolunteerApplicationsByCustomerIdAndStatus(Long customerId, VolunteerStatus status);

    /**
     * Đếm số lượng volunteer applications approved cho một event
     */
    long countApprovedVolunteersByEventId(Long eventId);

    /**
     * Đếm số lượng volunteer applications pending cho một event
     */
    long countPendingApplicationsByEventId(Long eventId);

    /**
     * Xóa volunteer application
     */
    void deleteVolunteerApplication(Long applicationId);
}

