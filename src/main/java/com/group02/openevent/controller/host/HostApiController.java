package com.group02.openevent.controller.host;

import com.group02.openevent.dto.request.HostRegistrationRequest;
import com.group02.openevent.model.organization.Organization;
import com.group02.openevent.model.user.Customer;
import com.group02.openevent.model.user.User;
import com.group02.openevent.repository.IAccountRepo;
import com.group02.openevent.repository.ICustomerRepo;
import com.group02.openevent.service.CustomerService;
import com.group02.openevent.service.HostService;
import com.group02.openevent.service.OrganizationService;
import com.group02.openevent.service.UserService;
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
    private final UserService userService;

    public HostApiController(HostService hostService,
                             ICustomerRepo customerRepo, OrganizationService organizationService,
                             CustomerService customerService, IAccountRepo accountRepo, UserService userService) {
        this.hostService = hostService;
        this.customerRepo = customerRepo;
        this.organizationService = organizationService;
        this.customerService = customerService;
        this.accountRepo = accountRepo;
        this.userService = userService;
    }

    /**
     * Kiểm tra xem user hiện tại đã là host chưa
     * Account-User có thể có 3 role: Customer, Department, Host
     */
    @GetMapping("/check")
    public ResponseEntity<Map<String, Object>> checkHostStatus(HttpSession session) {
        try {
            User user = userService.getCurrentUser(session);
            if (user == null) {
                return ResponseEntity.ok(Map.of(
                    "authenticated", false, 
                    "isHost", false, 
                    "hasCustomer", false,
                    "hasDepartment", false
                ));
            }

            // Chỉ check user có phải host hay không (không liên quan customer)
            boolean isHost = user.hasHostRole() || hostService.isUserHost(user.getUserId());
            boolean hasCustomer = user.hasCustomerRole();
            boolean hasDepartment = user.hasDepartmentRole();
            
            Map<String, Object> response = new HashMap<>();
            response.put("authenticated", true);
            response.put("isHost", isHost);
            response.put("hasCustomer", hasCustomer);
            response.put("hasDepartment", hasDepartment);
            response.put("userId", user.getUserId());
            
            // Nếu là customer, thêm customerId (giữ lại để frontend có thể dùng)
            if (hasCustomer && user.getCustomer() != null) {
                response.put("customerId", user.getCustomer().getCustomerId());
            }
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                "authenticated", true, 
                "isHost", false, 
                "error", e.getMessage()
            ));
        }
    }

    /**
     * Đăng ký làm host
     */
    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> registerHost(
            @RequestBody HostRegistrationRequest request,
            HttpSession session) {

        try {
            User user = userService.getCurrentUser(session);
            if (user == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "User not authenticated"));
            }
            
            // BỎ check customer - không cần customer để đăng ký làm host
            // Chỉ check user đã là host chưa
            if (hostService.isUserHost(user.getUserId())) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "User is already a host"));
            }
            
            // Đăng ký host trực tiếp từ User (không cần customer)
            hostService.registerHost(user, request);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Host registration successful");
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

