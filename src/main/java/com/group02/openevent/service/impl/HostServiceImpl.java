package com.group02.openevent.service.impl;

import com.group02.openevent.dto.request.HostRegistrationRequest;
import com.group02.openevent.model.account.Account;
import com.group02.openevent.model.enums.Role;
import com.group02.openevent.model.organization.Organization;
import com.group02.openevent.model.user.Customer;
import com.group02.openevent.model.user.Host;
import com.group02.openevent.repository.IAccountRepo;
import com.group02.openevent.repository.ICustomerRepo;
import com.group02.openevent.repository.IHostRepo;
import com.group02.openevent.service.HostService;
import com.group02.openevent.service.OrganizationService;
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
    
    public HostServiceImpl(IHostRepo hostRepo, OrganizationService organizationService, 
                          IAccountRepo accountRepo, ICustomerRepo customerRepo) {
        this.hostRepo = hostRepo;
        this.organizationService = organizationService;
        this.accountRepo = accountRepo;
        this.customerRepo = customerRepo;
    }
    
    @Override
    public Optional<Host> findByCustomerId(Long customerId) {
        return hostRepo.findByCustomer_CustomerId(customerId);
    }
    
    @Override
    public Host save(Host host) {
        return hostRepo.save(host);
    }

    @Override
    public Long findHostIdByAccountId(Long accountId) {
        Host host = hostRepo.findByAccountId(accountId)
                .orElseThrow(() -> new RuntimeException("This account is not a host"));
        return host.getId();
    }

    @Override
    public boolean isCustomerHost(Long customerId) {
        return hostRepo.findByCustomer_CustomerId(customerId).isPresent();
    }

    @Override
    public Host registerHost(Customer customer, HostRegistrationRequest request) {
        // Kiểm tra xem customer đã là host chưa
        if (isCustomerHost(customer.getCustomerId())) {
            throw new RuntimeException("Customer is already a host");
        }

        // Set host name vào Customer.name nếu có
        if (request.getHostName() != null && !request.getHostName().trim().isEmpty()) {
            customer.setName(request.getHostName().trim());
        } else if (customer.getName() == null || customer.getName().trim().isEmpty()) {
            // Nếu không có hostName và customer chưa có name, dùng email username
            if (customer.getAccount() != null && customer.getAccount().getEmail() != null) {
                String email = customer.getAccount().getEmail();
                String username = email.contains("@") ? email.split("@")[0] : email;
                customer.setName(username);
            }
        }

        // Tạo host mới
        Host host = new Host();
        host.setCustomer(customer);
        host.setCreatedAt(LocalDateTime.now());
        
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
        
        // Update Account role thành HOST
        Account account = customer.getAccount();
        if (account != null) {
            // Nếu role hiện tại là CUSTOMER, thì set thành HOST
            // Nếu đã là HOST hoặc ADMIN thì giữ nguyên
            if (account.getRole() == Role.CUSTOMER) {
                account.setRole(Role.HOST);
                accountRepo.save(account);
            }
        }
        
        // Lưu customer (với name đã được update) trước khi save host
        customerRepo.save(customer);
        
        // Lưu host
        return hostRepo.save(host);
    }
}