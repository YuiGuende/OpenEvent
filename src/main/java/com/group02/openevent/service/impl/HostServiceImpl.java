package com.group02.openevent.service.impl;

import com.group02.openevent.dto.request.HostRegistrationRequest;
import com.group02.openevent.model.organization.Organization;
import com.group02.openevent.model.user.Customer;
import com.group02.openevent.model.user.Host;
import com.group02.openevent.model.user.User;
import com.group02.openevent.repository.IAccountRepo;
import com.group02.openevent.repository.ICustomerRepo;
import com.group02.openevent.repository.IHostRepo;
import com.group02.openevent.service.EventChatService;
import com.group02.openevent.service.HostService;
import com.group02.openevent.service.OrganizationService;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
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
    private final EventChatService eventChatService;

    public HostServiceImpl(IHostRepo hostRepo, OrganizationService organizationService,
                           IAccountRepo accountRepo, ICustomerRepo customerRepo,
                           EventChatService eventChatService) {
        this.hostRepo = hostRepo;
        this.organizationService = organizationService;
        this.accountRepo = accountRepo;
        this.customerRepo = customerRepo;
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
    public boolean isUserHost(Long customerId) {
        return hostRepo.findByUser_UserId(customerId).isPresent();
    }

    @Override
    public Host registerHost(Customer customer, HostRegistrationRequest request) {
        // Kiểm tra xem customer đã là host chưa
        if (isUserHost(customer.getCustomerId())) {
            throw new RuntimeException("Customer is already a host");
        }

        // Set host name vào Customer.name nếu có
        if (request.getHostName() != null && !request.getHostName().trim().isEmpty()) {
            customer.getUser().setName(request.getHostName().trim());
        } else if (customer.getUser().getName() == null || customer.getUser().getName().trim().isEmpty()) {
            // Nếu không có hostName và customer chưa có name, dùng email username
            if (customer.getUser().getAccount() != null && customer.getUser().getAccount().getEmail() != null) {
                String email = customer.getUser().getAccount().getEmail();
                String username = email.contains("@") ? email.split("@")[0] : email;
                customer.getUser().setName(username);
            }
        }

        // Tạo host mới
        Host host = new Host();
        host.setCreatedAt(LocalDateTime.now());
        host.setUser(customer.getUser());
        // Set organization nếu có
        if (request.getOrganizationId() != null) {
            Optional<Organization> organization = organizationService.findById(request.getOrganizationId());
            if (organization.isPresent()) {
                host.setOrganization(organization.get());
            }
        }

        // Set discount percent (default 0 nếu null)
        if (request.getHostDiscountPercent() != null) {
            host.setHostDiscountPercent(request.getHostDiscountPercent());
        } else {
            host.setHostDiscountPercent(BigDecimal.ZERO);
        }
        // Set description
        host.setDescription(request.getDescription());
        // Lưu customer (với name đã được update) trước khi save host
        customerRepo.save(customer);

        // Lưu host
        Host savedHost = hostRepo.save(host);
        
        // Tự động tạo room chat với department
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