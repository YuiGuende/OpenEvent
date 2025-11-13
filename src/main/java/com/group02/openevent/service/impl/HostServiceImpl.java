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
import com.group02.openevent.service.HostService;
import com.group02.openevent.service.OrganizationService;
import jakarta.servlet.http.HttpSession;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@Transactional
public class HostServiceImpl implements HostService {
    private final IHostRepo hostRepo;
    private final OrganizationService organizationService;
    private final IAccountRepo accountRepo;
    private final ICustomerRepo customerRepo;
    private final ApplicationEventPublisher eventPublisher;

    public HostServiceImpl(IHostRepo hostRepo, OrganizationService organizationService,
                           IAccountRepo accountRepo, ICustomerRepo customerRepo, ApplicationEventPublisher eventPublisher) {
        this.hostRepo = hostRepo;
        this.organizationService = organizationService;
        this.accountRepo = accountRepo;
        this.customerRepo = customerRepo;
        this.eventPublisher = eventPublisher;
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

        // Set host name vào Customer.name nếu có
//        if (request.getHostName() != null && !request.getHostName().trim().isEmpty()) {
//            user.setName(request.getHostName().trim());
//        } else if (user.getName() == null || user.getName().trim().isEmpty()) {
//            // Nếu không có hostName và user chưa có name, dùng email username
//            if (user.getAccount() != null && user.getAccount().getEmail() != null) {
//                String email = user.getAccount().getEmail();
//                String username = email.contains("@") ? email.split("@")[0] : email;
//                user.setName(username);
//            }
//        }

        // Tạo host mới
        Host host = new Host();
        host.setCreatedAt(LocalDateTime.now());
        host.setUser(user);
        // Set organization nếu có
//        if (request.getOrganizationId() != null) {
//            Optional<Organization> organization = organizationService.findById(request.getOrganizationId());
//            organization.ifPresent(host::setOrganization);
//        }

        // Set discount percent (default 0 nếu null)
//        if (request.getHostDiscountPercent() != null) {
//            host.setHostDiscountPercent(request.getHostDiscountPercent());
//        } else {
//            host.setHostDiscountPercent(BigDecimal.ZERO);
//        }
        // Set description
        host.setDescription(request.getDescription());
        // Lưu user (với name đã được update) trước khi save host
//        customerRepo.save(user);

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
        
        return savedHost;
    }
}