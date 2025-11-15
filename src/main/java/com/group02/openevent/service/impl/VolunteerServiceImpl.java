package com.group02.openevent.service.impl;

import com.group02.openevent.model.account.Account;
import com.group02.openevent.model.event.Event;
import com.group02.openevent.model.user.Customer;
import com.group02.openevent.model.volunteer.VolunteerApplication;
import com.group02.openevent.model.volunteer.VolunteerStatus;
import com.group02.openevent.repository.IAccountRepo;
import com.group02.openevent.repository.ICustomerRepo;
import com.group02.openevent.repository.IEventRepo;
import com.group02.openevent.repository.IVolunteerApplicationRepo;
import com.group02.openevent.repository.EventChatRoomRepository;
import com.group02.openevent.model.chat.ChatRoomType;
import com.group02.openevent.model.chat.EventChatRoom;
import com.group02.openevent.service.EventChatService;
import com.group02.openevent.service.VolunteerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class VolunteerServiceImpl implements VolunteerService {

    private final IVolunteerApplicationRepo volunteerApplicationRepo;
    private final ICustomerRepo customerRepo;
    private final IEventRepo eventRepo;
    private final IAccountRepo accountRepo;
    private final com.group02.openevent.repository.IOrderRepo orderRepo;
    private final EventChatRoomRepository eventChatRoomRepo;
    private final EventChatService eventChatService;
    
    // ✅ Sử dụng constructor injection với @Lazy để phá vỡ circular dependency
    @Autowired
    public VolunteerServiceImpl(
            IVolunteerApplicationRepo volunteerApplicationRepo,
            ICustomerRepo customerRepo,
            IEventRepo eventRepo,
            IAccountRepo accountRepo,
            com.group02.openevent.repository.IOrderRepo orderRepo,
            EventChatRoomRepository eventChatRoomRepo,
            @Lazy EventChatService eventChatService) {
        this.volunteerApplicationRepo = volunteerApplicationRepo;
        this.customerRepo = customerRepo;
        this.eventRepo = eventRepo;
        this.accountRepo = accountRepo;
        this.orderRepo = orderRepo;
        this.eventChatRoomRepo = eventChatRoomRepo;
        this.eventChatService = eventChatService;
    }

    @Override
    @Transactional
    public VolunteerApplication createVolunteerApplication(Long customerId, Long eventId, String applicationMessage) {
        log.info("Creating volunteer application for customer {} and event {}", customerId, eventId);

        // Kiểm tra customer đã apply chưa
        if (volunteerApplicationRepo.existsByCustomer_CustomerIdAndEvent_Id(customerId, eventId)) {
            throw new IllegalStateException("Customer đã đăng ký làm volunteer cho event này rồi");
        }

        // Không cho apply nếu đã có order PAID (đăng ký làm customer)
        boolean hasPaidOrder = orderRepo.existsPaidByEventIdAndCustomerId(eventId, customerId);
        if (hasPaidOrder) {
            throw new IllegalStateException("Bạn đã đăng ký tham gia sự kiện này với tư cách khách hàng");
        }

        Customer customer = customerRepo.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + customerId));

        Event event = eventRepo.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found: " + eventId));

        VolunteerApplication application = new VolunteerApplication(customer, event);
        application.setApplicationMessage(applicationMessage);
        application.setStatus(VolunteerStatus.PENDING);

        VolunteerApplication saved = volunteerApplicationRepo.save(application);
        log.info("Volunteer application created with ID: {}", saved.getVolunteerApplicationId());
        
        return saved;
    }

    @Override
    @Transactional
    public VolunteerApplication approveVolunteerApplication(Long applicationId, Long reviewerAccountId, String hostResponse) {
        log.info("Approving volunteer application {} by reviewer {}", applicationId, reviewerAccountId);

        VolunteerApplication application = volunteerApplicationRepo.findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("Volunteer application not found: " + applicationId));

        if (application.getStatus() != VolunteerStatus.PENDING) {
            throw new IllegalStateException("Chỉ có thể approve application đang ở trạng thái PENDING");
        }

        Account reviewer = accountRepo.findById(reviewerAccountId)
                .orElseThrow(() -> new IllegalArgumentException("Reviewer account not found: " + reviewerAccountId));

        application.setStatus(VolunteerStatus.APPROVED);
        application.setReviewedBy(reviewer);
        application.setReviewedAt(LocalDateTime.now());
        application.setHostResponse(hostResponse);

        VolunteerApplication saved = volunteerApplicationRepo.save(application);
        log.info("Volunteer application {} approved successfully", applicationId);
        
        // ✅ Tự động thêm volunteer vào HOST_VOLUNTEERS room nếu room đã tồn tại
        try {
            Event event = saved.getEvent();
            Customer customer = saved.getCustomer();
            
            if (event != null && event.getHost() != null && customer != null && customer.getUser() != null) {
                Long hostUserId = event.getHost().getUser().getUserId();
                Long volunteerUserId = customer.getUser().getUserId();
                
                // Tìm HOST_VOLUNTEERS room cho event này
                Optional<EventChatRoom> existingRoom = 
                    eventChatRoomRepo.findByEventAndHostAndRoomType(event.getId(), hostUserId);
                
                if (existingRoom.isPresent()) {
                    EventChatRoom room = existingRoom.get();
                    if (room.getRoomType() == ChatRoomType.HOST_VOLUNTEERS) {
                        // Tự động thêm volunteer vào room
                        eventChatService.addParticipantToRoom(room.getId(), volunteerUserId);
                        log.info("Auto-added approved volunteer {} to HOST_VOLUNTEERS room {} for event {}", 
                            volunteerUserId, room.getId(), event.getId());
                    }
                }
            }
        } catch (Exception e) {
            // Log error nhưng không fail transaction nếu có lỗi khi thêm vào room
            log.error("Error auto-adding volunteer to chat room after approval: {}", e.getMessage(), e);
        }
        
        return saved;
    }

    @Override
    @Transactional
    public VolunteerApplication rejectVolunteerApplication(Long applicationId, Long reviewerAccountId, String hostResponse) {
        log.info("Rejecting volunteer application {} by reviewer {}", applicationId, reviewerAccountId);

        VolunteerApplication application = volunteerApplicationRepo.findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("Volunteer application not found: " + applicationId));

        if (application.getStatus() != VolunteerStatus.PENDING) {
            throw new IllegalStateException("Chỉ có thể reject application đang ở trạng thái PENDING");
        }

        Account reviewer = accountRepo.findById(reviewerAccountId)
                .orElseThrow(() -> new IllegalArgumentException("Reviewer account not found: " + reviewerAccountId));

        application.setStatus(VolunteerStatus.REJECTED);
        application.setReviewedBy(reviewer);
        application.setReviewedAt(LocalDateTime.now());
        application.setHostResponse(hostResponse);

        VolunteerApplication saved = volunteerApplicationRepo.save(application);
        log.info("Volunteer application {} rejected", applicationId);
        
        return saved;
    }

    @Override
    public Optional<VolunteerApplication> getVolunteerApplicationById(Long applicationId) {
        return volunteerApplicationRepo.findById(applicationId);
    }

    @Override
    public List<VolunteerApplication> getVolunteerApplicationsByEventId(Long eventId) {
        return volunteerApplicationRepo.findByEvent_Id(eventId);
    }

    @Override
    public List<VolunteerApplication> getVolunteerApplicationsByEventIdAndStatus(Long eventId, VolunteerStatus status) {
        return volunteerApplicationRepo.findByEvent_IdAndStatus(eventId, status);
    }

    @Override
    public Optional<VolunteerApplication> getVolunteerApplicationByCustomerAndEvent(Long customerId, Long eventId) {
        return volunteerApplicationRepo.findByCustomer_CustomerIdAndEvent_Id(customerId, eventId);
    }

    @Override
    public boolean hasCustomerAppliedAsVolunteer(Long customerId, Long eventId) {
        return volunteerApplicationRepo.existsByCustomer_CustomerIdAndEvent_Id(customerId, eventId);
    }

    @Override
    public boolean isCustomerApprovedVolunteer(Long customerId, Long eventId) {
        Optional<VolunteerApplication> application = volunteerApplicationRepo.findByCustomer_CustomerIdAndEvent_Id(customerId, eventId);
        return application.isPresent() && application.get().getStatus() == VolunteerStatus.APPROVED;
    }

    @Override
    public List<VolunteerApplication> getVolunteerApplicationsByCustomerId(Long customerId) {
        return volunteerApplicationRepo.findByCustomer_CustomerId(customerId);
    }

    @Override
    public List<VolunteerApplication> getVolunteerApplicationsByCustomerIdAndStatus(Long customerId, VolunteerStatus status) {
        return volunteerApplicationRepo.findByCustomer_CustomerIdAndStatus(customerId, status);
    }

    @Override
    public long countApprovedVolunteersByEventId(Long eventId) {
        return volunteerApplicationRepo.countByEvent_IdAndStatus(eventId, VolunteerStatus.APPROVED);
    }

    @Override
    public long countPendingApplicationsByEventId(Long eventId) {
        return volunteerApplicationRepo.countPendingApplicationsByEventId(eventId);
    }

    @Override
    @Transactional
    public void deleteVolunteerApplication(Long applicationId) {
        log.info("Deleting volunteer application {}", applicationId);
        volunteerApplicationRepo.deleteById(applicationId);
    }
}

