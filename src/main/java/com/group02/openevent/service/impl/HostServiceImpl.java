package com.group02.openevent.service.impl;

import com.group02.openevent.dto.request.HostRegistrationRequest;
import com.group02.openevent.model.organization.Organization;
import com.group02.openevent.model.user.Customer;
import com.group02.openevent.model.user.Host;
import com.group02.openevent.model.user.User;
import com.group02.openevent.event.UserCreatedEvent;
import com.group02.openevent.repository.IAccountRepo;
import com.group02.openevent.repository.ICustomerRepo;
import com.group02.openevent.repository.IHostRepo;
import com.group02.openevent.service.EventChatService;
import com.group02.openevent.service.HostService;
import com.group02.openevent.service.OrganizationService;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@Transactional
@Slf4j
public class HostServiceImpl implements HostService {
    private final IHostRepo hostRepo;
    private final OrganizationService organizationService;
    private final IAccountRepo accountRepo;
    private final ICustomerRepo customerRepo;
    private final ApplicationEventPublisher eventPublisher;
    private final EventChatService eventChatService;

    public HostServiceImpl(IHostRepo hostRepo, OrganizationService organizationService,
                           IAccountRepo accountRepo, ICustomerRepo customerRepo, ApplicationEventPublisher eventPublisher, EventChatService eventChatService) {
        this.hostRepo = hostRepo;
        this.organizationService = organizationService;
        this.accountRepo = accountRepo;
        this.customerRepo = customerRepo;
        this.eventPublisher = eventPublisher;
        this.eventChatService = eventChatService;
    }
    
    @Override
    public Host findByCustomerId(Long customerId) {
        Optional<Customer> customer=customerRepo.findById(customerId);
        User user=null;
        if(customer.isPresent()){
            user=customer.get().getUser();
        }else{
            throw new RuntimeException("Customer not found");
        }

        return user.getHost();
    }
    
    @Override
    public Host save(Host host) {
        return hostRepo.save(host);
    }


    @Override
    public Host findHostByUserId(Long userId) {
        return hostRepo.findByUser_UserId(userId)
                .orElseThrow(() -> new RuntimeException("This account is not a host"));
    }

    @Override
    public Host getHostFromSession(HttpSession session) {
        Long userId = (Long) session.getAttribute("USER_ID");
        if (userId == null) {
            throw new RuntimeException("User not logged in");
        }
        return findHostByUserId(userId);
    }

    @Override
    public boolean isUserHost(Long userId) {
        return hostRepo.findByUser_UserId(userId).isPresent();
    }

    @Override
    public Host registerHost(User user, HostRegistrationRequest request) {
        // Kiểm tra xem user đã là host chưa
        if (isUserHost(user.getUserId())) {
            throw new RuntimeException("User is already a host");
        }

        // Tạo host mới
        Host host = new Host();
        host.setCreatedAt(LocalDateTime.now());
        host.setUser(user);
        host.setDescription(request.getDescription());

        // Lưu host
        Host savedHost = hostRepo.save(host);

        // Publish UserCreatedEvent for audit log (user registered as HOST)
        try {
            Long actorId = user != null
                    ? user.getUserId()
                    : null;
            if (actorId != null) {
                eventPublisher.publishEvent(new UserCreatedEvent(this, user, actorId, "HOST"));
            }
        } catch (Exception e) {
            System.err.println("Error publishing UserCreatedEvent for HOST: " + e.getMessage());
        }

        try {
            eventChatService.createHostDepartmentRoom(savedHost.getUser().getUserId());
            log.info("Successfully created Host-Department chat room for host: {}", savedHost.getUser().getUserId());
        } catch (Exception e) {
            log.warn("Failed to create host-department chat room: {}", e.getMessage());
            // Không throw exception, chỉ log warning để không ảnh hưởng đến quá trình register host
        }



        return savedHost;
    }
}