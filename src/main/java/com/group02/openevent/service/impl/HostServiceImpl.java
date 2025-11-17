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
import com.group02.openevent.repository.IUserRepo;
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
import java.util.List;
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
    private final IUserRepo userRepo;

    public HostServiceImpl(IHostRepo hostRepo, OrganizationService organizationService,
                           IAccountRepo accountRepo, ICustomerRepo customerRepo, ApplicationEventPublisher eventPublisher, EventChatService eventChatService, IUserRepo userRepo) {

        this.hostRepo = hostRepo;
        this.organizationService = organizationService;
        this.accountRepo = accountRepo;
        this.customerRepo = customerRepo;
        this.userRepo = userRepo;
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
        List<Host> hosts = hostRepo.findAllByUser_UserId(userId);
        if (hosts.isEmpty()) {
            throw new RuntimeException("This account is not a host");
        }
        // Return first host for backward compatibility
        // In the future, you may want to select by organization or other criteria
        return hosts.get(0);
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
        List<Host> hosts = hostRepo.findAllByUser_UserId(userId);
        return hosts != null && !hosts.isEmpty();
    }


    @Override
    public Host registerHost(User user, HostRegistrationRequest request) {
        // Note: Một user có thể có nhiều host records (cho các events khác nhau)
        // Không cần check isUserHost() nữa vì cho phép nhiều hosts

        // Tạo host mới
        Host host = new Host();
        host.setCreatedAt(LocalDateTime.now());
        host.setUser(user); // Map trực tiếp với User

        // Set host name riêng vào Host.hostName (không ảnh hưởng đến User.name/Customer)
        if (request.getHostName() != null && !request.getHostName().trim().isEmpty()) {
            host.setHostName(request.getHostName().trim());
        } else if (user.getName() != null && !user.getName().trim().isEmpty()) {
            // Nếu không có hostName trong request, dùng User.name làm mặc định
            host.setHostName(user.getName());
        } else {
            // Nếu User chưa có name, dùng email username
            if (user.getAccount() != null && user.getAccount().getEmail() != null) {
                String email = user.getAccount().getEmail();
                String username = email.contains("@") ? email.split("@")[0] : email;
                host.setHostName(username);
            }
        }

        // Set organization nếu có

        // Set discount percent (default 0 nếu null)
//        if (request.getHostDiscountPercent() != null) {
//            host.setHostDiscountPercent(request.getHostDiscountPercent());
//        } else {
//            host.setHostDiscountPercent(BigDecimal.ZERO);
//        }

        // Set description
        host.setDescription(request.getDescription());

        // Lưu user (với name đã được update) trước khi save host
        userRepo.save(user);

        // Lưu host
        return hostRepo.save(host);
    }
}