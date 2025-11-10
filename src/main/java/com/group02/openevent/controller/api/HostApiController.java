package com.group02.openevent.controller.api;

import com.group02.openevent.dto.request.HostRegistrationRequest;
import com.group02.openevent.model.account.Account;
import com.group02.openevent.model.organization.Organization;
import com.group02.openevent.model.user.Customer;
import com.group02.openevent.repository.IAccountRepo;
import com.group02.openevent.repository.ICustomerRepo;
import com.group02.openevent.service.CustomerService;
import com.group02.openevent.service.HostService;
import com.group02.openevent.service.OrganizationService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/host")
public class HostApiController {

    private final HostService hostService;
    private final ICustomerRepo customerRepo;
    private final OrganizationService organizationService;
    private final CustomerService customerService;
    private final IAccountRepo accountRepo;

    public HostApiController(HostService hostService, 
                            ICustomerRepo customerRepo, OrganizationService organizationService,
                            CustomerService customerService, IAccountRepo accountRepo) {
        this.hostService = hostService;
        this.customerRepo = customerRepo;
        this.organizationService = organizationService;
        this.customerService = customerService;
        this.accountRepo = accountRepo;
    }

    /**
     * Kiểm tra xem user hiện tại đã là host chưa
     */
    @GetMapping("/check")
    public ResponseEntity<Map<String, Object>> checkHostStatus(HttpSession session) {
        Long accountId = (Long) session.getAttribute("ACCOUNT_ID");
        if (accountId == null) {
            return ResponseEntity.ok(Map.of("authenticated", false, "isHost", false));
        }

        try {
            Customer customer = customerRepo.findByAccount_AccountId(accountId).orElse(null);
            if (customer == null) {
                return ResponseEntity.ok(Map.of("authenticated", true, "isHost", false, "hasCustomer", false));
            }

            boolean isHost = hostService.isCustomerHost(customer.getCustomerId());
            return ResponseEntity.ok(Map.of(
                    "authenticated", true,
                    "isHost", isHost,
                    "hasCustomer", true,
                    "customerId", customer.getCustomerId()
            ));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("authenticated", true, "isHost", false, "error", e.getMessage()));
        }
    }

    /**
     * Đăng ký làm host
     */
    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> registerHost(
            @RequestBody HostRegistrationRequest request,
            HttpSession session) {
        
        Long accountId = (Long) session.getAttribute("ACCOUNT_ID");
        if (accountId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "User not authenticated"));
        }

        try {
            // Lấy customer từ accountId
            Customer customer = customerRepo.findByAccount_AccountId(accountId)
                    .orElseThrow(() -> new RuntimeException("Customer not found for this account"));

            // Kiểm tra xem đã là host chưa
            if (hostService.isCustomerHost(customer.getCustomerId())) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Customer is already a host"));
            }

            // Đăng ký host (sẽ update customer name và account role, và save cả customer và account)
            hostService.registerHost(customer, request);

            // Reload account để lấy role mới nhất
            Account account = accountRepo.findById(accountId).orElse(null);
            if (account != null) {
                // Update session với role mới
                session.setAttribute("ACCOUNT_ROLE", account.getRole().name());
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Host registration successful");
            response.put("role", account != null ? account.getRole().name() : "HOST");
            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "An error occurred during host registration: " + e.getMessage()));
        }
    }

    /**
     * Lấy danh sách organizations để hiển thị trong dropdown
     */
    @GetMapping("/organizations")
    public ResponseEntity<List<Map<String, Object>>> getOrganizations() {
        try {
            List<Organization> organizations = organizationService.findAll();
            List<Map<String, Object>> orgList = organizations.stream()
                    .map(org -> {
                        Map<String, Object> orgMap = new HashMap<>();
                        orgMap.put("id", org.getOrgId());
                        orgMap.put("name", org.getOrgName());
                        return orgMap;
                    })
                    .collect(Collectors.toList());
            return ResponseEntity.ok(orgList);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}

