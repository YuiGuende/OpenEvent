package com.group02.openevent.service.impl;

import com.group02.openevent.dto.request.HostRegistrationRequest;
import com.group02.openevent.model.organization.Organization;
import com.group02.openevent.model.user.Customer;
import com.group02.openevent.model.user.Host;
import com.group02.openevent.model.user.User;
import com.group02.openevent.repository.IAccountRepo;
import com.group02.openevent.repository.ICustomerRepo;
import com.group02.openevent.repository.IHostRepo;
import com.group02.openevent.repository.IUserRepo;
import com.group02.openevent.service.HostService;
import com.group02.openevent.service.OrganizationService;
import jakarta.servlet.http.HttpSession;
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
    private final IUserRepo userRepo;

    public HostServiceImpl(IHostRepo hostRepo, OrganizationService organizationService,
                           IAccountRepo accountRepo, ICustomerRepo customerRepo, IUserRepo userRepo) {
        this.hostRepo = hostRepo;
        this.organizationService = organizationService;
        this.accountRepo = accountRepo;
        this.customerRepo = customerRepo;
        this.userRepo = userRepo;
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
        if (request.getOrganizationId() != null) {
            Optional<Organization> organization = organizationService.findById(request.getOrganizationId());
            if (organization.isPresent()) {
                host.setOrganization(organization.get());
            }
        }

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