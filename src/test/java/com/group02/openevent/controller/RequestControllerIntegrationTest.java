package com.group02.openevent.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.group02.openevent.dto.requestApproveEvent.ApproveRequestDTO;
// IMPORTS MỚI
import com.group02.openevent.model.account.Account;
import com.group02.openevent.model.department.Department;
import com.group02.openevent.model.enums.Role;
import com.group02.openevent.model.user.Customer;
import com.group02.openevent.model.user.Host;
import com.group02.openevent.repository.ICustomerRepo; // Cần repo này
import com.group02.openevent.repository.IHostRepo;     // Cần repo này
// ---
import com.group02.openevent.model.enums.EventStatus;
import com.group02.openevent.model.event.Event;
import com.group02.openevent.model.request.Request;
import com.group02.openevent.model.request.RequestStatus;
import com.group02.openevent.model.request.RequestType;
import com.group02.openevent.repository.IAccountRepo;
import com.group02.openevent.repository.IDepartmentRepo;
import com.group02.openevent.repository.IEventRepo;
import com.group02.openevent.repository.IRequestRepo;
import com.group02.openevent.util.CloudinaryUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime; // <-- THÊM IMPORT

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
// THÊM IMPORT ĐỂ GIẢ LẬP LOGIN
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("Integration Test cho RequestController (AOP & Service)")
class RequestControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    // Dùng repo thật để setup CSDL
    @Autowired
    private IAccountRepo accountRepo;
    @Autowired
    private IDepartmentRepo departmentRepo;
    @Autowired
    private IEventRepo eventRepo;
    @Autowired
    private IRequestRepo requestRepo;

    // === REPOS MỚI ===
    @Autowired
    private ICustomerRepo customerRepo;
    @Autowired
    private IHostRepo hostRepo;

    @MockBean
    private CloudinaryUtil cloudinaryUtil;

    // Dữ liệu setup
    private Host hostA, hostB;
    private Department deptD1, deptD2;
    private Event eventE1;

    // === PHẦN ĐƯỢC CẬP NHẬT HOÀN TOÀN ===
    @BeforeEach
    void setUpDatabase() throws IOException {
        when(cloudinaryUtil.uploadFile(any())).thenReturn("http://mock-url.com");

        // 1. Tạo Host A (Account -> Customer -> Host)
        Account accountA = new Account();
        accountA.setEmail("hostA@mail.com");
        accountA.setPasswordHash("mockpass");
        accountA.setRole(Role.CUSTOMER);
        accountA = accountRepo.save(accountA);

        Customer customerA = new Customer();
        customerA.setAccount(accountA);
        customerA.setName("Host A");
        customerA = customerRepo.save(customerA);

        hostA = new Host();
        hostA.setCustomer(customerA);
        hostA = hostRepo.save(hostA);

        // 2. Tạo Host B (Account -> Customer -> Host)
        Account accountB = new Account();
        accountB.setEmail("hostB@mail.com");
        accountB.setPasswordHash("mockpass");
        accountB.setRole(Role.CUSTOMER);
        accountB = accountRepo.save(accountB);

        Customer customerB = new Customer();
        customerB.setAccount(accountB);
        customerB.setName("Host B");
        customerB = customerRepo.save(customerB);

        hostB = new Host();
        hostB.setCustomer(customerB);
        hostB = hostRepo.save(hostB);

        // 3. Tạo Department D1 (Account -> Department)
        Account deptAccountD1 = new Account();
        deptAccountD1.setEmail("deptD1@mail.com");
        deptAccountD1.setPasswordHash("mockpass");
        deptAccountD1.setRole(Role.DEPARTMENT);
        deptAccountD1 = accountRepo.save(deptAccountD1);

        deptD1 = Department.builder()
                .account(deptAccountD1)
                .departmentName("Department D1")
                .build();
        deptD1 = departmentRepo.save(deptD1);

        // 4. Tạo Department D2
        Account deptAccountD2 = new Account();
        deptAccountD2.setEmail("deptD2@mail.com");
        deptAccountD2.setPasswordHash("mockpass");
        deptAccountD2.setRole(Role.DEPARTMENT);
        deptAccountD2 = accountRepo.save(deptAccountD2);

        deptD2 = Department.builder()
                .account(deptAccountD2)
                .departmentName("Department D2")
                .build();
        deptD2 = departmentRepo.save(deptD2);

        // 5. Tạo Event (do Host A sở hữu)
        eventE1 = new Event();
        eventE1.setTitle("Event E1");
        eventE1.setStatus(EventStatus.DRAFT);
        eventE1.setHost(hostA);

        // === SỬA LỖI: THÊM CÁC TRƯỜNG NOT NULL ===
        eventE1.setStartsAt(LocalDateTime.now().plusDays(1));
        eventE1.setEndsAt(LocalDateTime.now().plusDays(2));
        eventE1.setEnrollDeadline(LocalDateTime.now().plusDays(2));
        eventE1.setCapacity(100); // Ví dụ, thêm các trường NOT NULL khác nếu cần

        eventE1 = eventRepo.save(eventE1);
    }
    // === KẾT THÚC PHẦN CẬP NHẬT ===

    @Nested
    @DisplayName("Feature: Request API Flow (Controller + AOP + Service)")
    class RequestApiFlowTests {

        @Test
        @DisplayName("INT-01 (Happy Path - Full Flow): Host tạo request và Department approve")
        void whenHostCreatesRequestAndReceiverApproves_thenFlowSucceeds() throws Exception {
            // === Phần 1: Host A (Account) tạo request ===
            MockMultipartFile file = new MockMultipartFile("file", "test.txt", "text/plain", "test data".getBytes());

            MvcResult createResult = mockMvc.perform(
                            multipart("/api/requests")
                                    // === SỬA LỖI: Thêm .with(user(...)) để giả lập login ===
                                    .file(file)
                                    .with(user(hostA.getCustomer().getAccount().getEmail()).roles(Role.CUSTOMER.name()))
                                    .param("receiverId", String.valueOf(deptD1.getAccountId()))
                                    .param("type", RequestType.EVENT_APPROVAL.name())
                                    .param("eventId", String.valueOf(eventE1.getId()))
                                    .sessionAttr("ACCOUNT_ID", hostA.getCustomer().getAccount().getAccountId())
                    )
                    .andExpect(status().isCreated()) // Sẽ là 201 (không còn 302)
                    .andExpect(jsonPath("$.status").value("PENDING"))
                    .andReturn();

            String jsonResponse = createResult.getResponse().getContentAsString();
            Long newRequestId = objectMapper.readTree(jsonResponse).get("requestId").asLong();

            // === Phần 2: Department D1 (Account) approve request ===
            ApproveRequestDTO approveDTO = new ApproveRequestDTO("Approved by Dept D1");

            mockMvc.perform(
                            put("/api/requests/{requestId}/approve", newRequestId)
                                    // === SỬA LỖI: Thêm .with(user(...)) để giả lập login ===
                                    .with(user(deptD1.getAccount().getEmail()).roles(Role.DEPARTMENT.name()))
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(approveDTO))
                                    .sessionAttr("ACCOUNT_ID", deptD1.getAccountId()) // Dept D1 đang login
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("APPROVED"));

            // === Phần 3: Xác minh (Verify) CSDL ===
            Event updatedEvent = eventRepo.findById(eventE1.getId()).orElseThrow();
            assertThat(updatedEvent.getStatus()).isEqualTo(EventStatus.PUBLIC);
        }

        @Test
        @DisplayName("INT-02 (AOP @RequireEventHost): Host B không thể tạo request cho Event của Host A")
        void whenWrongHostCreatesRequest_thenAopDeniesAccess() throws Exception {
            // Test này sẽ fail nếu Aspect chưa được sửa
            mockMvc.perform(
                            multipart("/api/requests")
                                    // === SỬA LỖI: Thêm .with(user(...)) để giả lập login ===
                                    .with(user(hostB.getCustomer().getAccount().getEmail()).roles(Role.CUSTOMER.name()))
                                    .param("receiverId", String.valueOf(deptD1.getAccountId()))
                                    .param("type", RequestType.EVENT_APPROVAL.name())
                                    .param("eventId", String.valueOf(eventE1.getId()))
                                    .sessionAttr("ACCOUNT_ID", hostB.getCustomer().getAccount().getAccountId())
                    )
                    .andExpect(status().isForbidden()); // Sẽ là 403 (không còn 302)
        }

        @Test
        @DisplayName("INT-03 (AOP @RequireRequestReceiver): Sender (Host A) không thể approve request của mình")
        void whenSenderTriesToApproveRequest_thenAopDeniesAccess() throws Exception {
            // Given: Tạo 1 request R1, sender=HostA(Account), receiver=DeptD1(Account)
            Request r1 = Request.builder()
                    .sender(hostA.getCustomer().getAccount())
                    .receiver(deptD1.getAccount())
                    .event(eventE1) // <-- SỬA LỖI: Liên kết Event
                    .status(RequestStatus.PENDING)
                    .type(RequestType.EVENT_APPROVAL)
                    .createdAt(LocalDateTime.now())
                    .build();
            r1 = requestRepo.save(r1);

            ApproveRequestDTO approveDTO = new ApproveRequestDTO("Self-approve?");

            mockMvc.perform(
                            put("/api/requests/{requestId}/approve", r1.getRequestId())
                                    // === SỬA LỖI: Thêm .with(user(...)) để giả lập login ===
                                    .with(user(hostA.getCustomer().getAccount().getEmail()).roles(Role.CUSTOMER.name()))
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(approveDTO))
                                    .sessionAttr("ACCOUNT_ID", hostA.getCustomer().getAccount().getAccountId())
                    )
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("INT-04 (AOP @RequireRequestReceiver - Chéo): Dept D2 không thể approve request của Dept D1")
        void whenWrongReceiverTriesToApproveRequest_thenAopDeniesAccess() throws Exception {
            // Given: Tạo 1 request R1, receiver=DeptD1
            Request r1 = Request.builder()
                    .sender(hostA.getCustomer().getAccount())
                    .receiver(deptD1.getAccount())
                    .event(eventE1) // <-- SỬA LỖI: Liên kết Event
                    .status(RequestStatus.PENDING)
                    .type(RequestType.EVENT_APPROVAL)
                    .createdAt(LocalDateTime.now())
                    .build();
            r1 = requestRepo.save(r1);

            ApproveRequestDTO approveDTO = new ApproveRequestDTO("Wrong dept");

            mockMvc.perform(
                            put("/api/requests/{requestId}/approve", r1.getRequestId())
                                    // === SỬA LỖI: Thêm .with(user(...)) để giả lập login ===
                                    .with(user(deptD2.getAccount().getEmail()).roles(Role.DEPARTMENT.name()))
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(approveDTO))
                                    .sessionAttr("ACCOUNT_ID", deptD2.getAccountId()) // Dept D2 (sai người nhận)
                    )
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("INT-05 (Edge Case - State): Approve request đã được approve ném ra 400 Bad Request")
        void whenApprovingAlreadyApprovedRequest_thenServiceThrowsException() throws Exception {
            // Given: Tạo 1 request R1, receiver=DeptD1, status=APPROVED
            Request r1 = Request.builder()
                    .sender(hostA.getCustomer().getAccount())
                    .receiver(deptD1.getAccount())
                    .event(eventE1) // <-- SỬA LỖI: Liên kết Event
                    .status(RequestStatus.APPROVED) // Đã được approve
                    .type(RequestType.EVENT_APPROVAL)
                    .createdAt(LocalDateTime.now())
                    .build();
            r1 = requestRepo.save(r1);

            ApproveRequestDTO approveDTO = new ApproveRequestDTO("Approve again");

            mockMvc.perform(
                            put("/api/requests/{requestId}/approve", r1.getRequestId())
                                    // === SỬA LỖI: Thêm .with(user(...)) để giả lập login ===
                                    .with(user(deptD1.getAccount().getEmail()).roles(Role.DEPARTMENT.name()))
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(approveDTO))
                                    .sessionAttr("ACCOUNT_ID", deptD1.getAccountId()) // Dept D1 (đúng người nhận)
                    )
                    .andExpect(status().isBadRequest());
        }
    }
}
