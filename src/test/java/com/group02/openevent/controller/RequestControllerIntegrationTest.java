package com.group02.openevent.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.group02.openevent.ai.security.AISecurityService;
import com.group02.openevent.ai.security.RateLimitingService;
import com.group02.openevent.dto.requestApproveEvent.ApproveRequestDTO;
// THÊM DTO NÀY
import com.group02.openevent.dto.requestApproveEvent.CreateRequestDTO;
import com.group02.openevent.model.account.Account;
import com.group02.openevent.model.department.Department;
import com.group02.openevent.model.enums.Role;
import com.group02.openevent.model.user.Customer;
import com.group02.openevent.model.user.Host;
import com.group02.openevent.model.user.User;
import com.group02.openevent.repository.ICustomerRepo;
import com.group02.openevent.repository.IHostRepo;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
// THÊM IMPORT CHO HAMCREST (ĐỂ FIX LỖI ASSERTION)
import static org.hamcrest.Matchers.hasItem;
// THÊM IMPORT ĐỂ GIẢ LẬP LOGIN
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
// THÊM IMPORT CHO GET VÀ POST
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
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
    // THÊM MỘT REQUEST MẪU ĐỂ TEST GET
    private Request sampleRequest;
    @MockitoBean
    private RateLimitingService rateLimitingService;

    @MockitoBean
    private AISecurityService aiSecurityService;
    @BeforeEach
    void setUpDatabase() throws IOException {
        when(cloudinaryUtil.uploadFile(any())).thenReturn("http://mock-url.com");

        // 1. Tạo Host A (Account -> Customer -> Host)
        Account accountA = new Account();
        accountA.setEmail("hostA@mail.com");
        accountA.setPasswordHash("mockpass");
        accountA = accountRepo.save(accountA);

        User userA = new User();
        userA.setAccount(accountA);
        userA.setUserId(1L);
        userA.setName("Host A");
        Customer customerA = new Customer();
        customerA.setUser(userA);
        customerA = customerRepo.save(customerA);

        hostA = new Host();
        hostA.setCustomer(customerA);
        hostA = hostRepo.save(hostA);

        // 2. Tạo Host B (Account -> Customer -> Host)
        Account accountB = new Account();
        accountB.setEmail("hostB@mail.com");
        accountB.setPasswordHash("mockpass");
        accountB = accountRepo.save(accountB);

        User userB = new User();
        userB.setAccount(accountB);
        userB.setUserId(2L);
        userB.setName("Host B");
        Customer customerB = new Customer();
        customerB.setUser(userB);
        customerB = customerRepo.save(customerB);

        hostB = new Host();
        hostB.setCustomer(customerB);
        hostB = hostRepo.save(hostB);

        // 3. Tạo Department D1 (Account -> Department)
        Account deptAccountD1 = new Account();
        deptAccountD1.setEmail("deptD1@mail.com");
        deptAccountD1.setPasswordHash("mockpass");
        deptAccountD1 = accountRepo.save(deptAccountD1);

        User deptUserD1 = new User();
        deptUserD1.setAccount(deptAccountD1);
        deptUserD1.setUserId(3L);
        deptD1 = Department.builder()
                .user(deptUserD1)
                .departmentName("Department D1")
                .build();
        deptD1 = departmentRepo.save(deptD1);

        // 4. Tạo Department D2
        Account deptAccountD2 = new Account();
        deptAccountD2.setEmail("deptD2@mail.com");
        deptAccountD2.setPasswordHash("mockpass");
        deptAccountD2 = accountRepo.save(deptAccountD2);

        User deptUserD2 = new User();
        deptUserD2.setAccount(deptAccountD2);
        deptUserD2.setUserId(4L);
        deptD2 = Department.builder()
                .user(deptUserD2)
                .departmentName("Department D2")
                .build();
        deptD2 = departmentRepo.save(deptD2);

        // 5. Tạo Event (do Host A sở hữu)
        eventE1 = new Event();
        eventE1.setTitle("Event E1");
        eventE1.setStatus(EventStatus.DRAFT);
        eventE1.setHost(hostA);
        eventE1.setStartsAt(LocalDateTime.now().plusDays(1));
        eventE1.setEndsAt(LocalDateTime.now().plusDays(2));
        eventE1.setEnrollDeadline(LocalDateTime.now().plusDays(2));
        eventE1.setCapacity(100);

        eventE1 = eventRepo.save(eventE1);

        // 6. THÊM REQUEST MẪU ĐỂ TEST CÁC ENDPOINT GET
        sampleRequest = Request.builder()
                .sender(hostA.getCustomer().getUser().getAccount())
                .receiver(deptD1.getUser().getAccount())
                .event(eventE1)
                .status(RequestStatus.PENDING)
                .type(RequestType.EVENT_APPROVAL)
                .createdAt(LocalDateTime.now())
                .build();
        sampleRequest = requestRepo.save(sampleRequest);
    }

    @Nested
    @DisplayName("Feature: Request API Flow (Approve/CreateFile)")
    class RequestApiFlowTests {
        // ... (TẤT CẢ 5 TEST CASE CŨ CỦA BẠN (INT-01 ĐẾN INT-05) ĐƯỢC GIỮ NGUYÊN TẠI ĐÂY) ...
        // (Bao gồm whenHostCreatesRequestAndReceiverApproves_thenFlowSucceeds, ...)
        // ...
        @Test
        @DisplayName("INT-01 (Happy Path - Full Flow): Host tạo request và Department approve")
        void whenHostCreatesRequestAndReceiverApproves_thenFlowSucceeds() throws Exception {
            // === Phần 1: Host A (Account) tạo request ===
            MockMultipartFile file = new MockMultipartFile("file", "test.txt", "text/plain", "test data".getBytes());

            MvcResult createResult = mockMvc.perform(
                            multipart("/api/requests")
                                    .file(file) // Gọi .file() trước
                                    .with(user(hostA.getCustomer().getUser().getAccount().getEmail()).roles(Role.CUSTOMER.name())) // .with() sau
                                    .param("receiverId", String.valueOf(deptD1.getUser().getAccount().getAccountId()))
                                    .param("type", RequestType.EVENT_APPROVAL.name())
                                    .param("eventId", String.valueOf(eventE1.getId()))
                                    .sessionAttr("ACCOUNT_ID", hostA.getCustomer().getUser().getAccount().getAccountId())
                    )
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.status").value("PENDING"))
                    .andReturn();

            String jsonResponse = createResult.getResponse().getContentAsString();
            Long newRequestId = objectMapper.readTree(jsonResponse).get("requestId").asLong();

            // === Phần 2: Department D1 (Account) approve request ===
            ApproveRequestDTO approveDTO = new ApproveRequestDTO("Approved by Dept D1");

            mockMvc.perform(
                            put("/api/requests/{requestId}/approve", newRequestId)
                                    .with(user(deptD1.getUser().getAccount().getEmail()).roles(Role.DEPARTMENT.name()))
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(approveDTO))
                                    .sessionAttr("ACCOUNT_ID", deptD1.getUser().getAccount().getAccountId())
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

            MockMultipartFile file = new MockMultipartFile("file", "test.txt", "text/plain", "test data".getBytes());

            mockMvc.perform(
                            multipart("/api/requests")
                                    .file(file) // Gọi .file() trước
                                    .with(user(hostB.getCustomer().getUser().getAccount().getEmail()).roles(Role.CUSTOMER.name())) // .with() sau
                                    .param("receiverId", String.valueOf(deptD1.getUser().getAccount().getAccountId()))
                                    .param("type", RequestType.EVENT_APPROVAL.name())
                                    .param("eventId", String.valueOf(eventE1.getId()))
                                    .sessionAttr("ACCOUNT_ID", hostB.getCustomer().getUser().getAccount().getAccountId())
                    )
                    .andExpect(status().isForbidden()); // Mong đợi 403 (Giả sử GlobalExceptionHandler đã được sửa)
        }

        @Test
        @DisplayName("INT-03 (AOP @RequireRequestReceiver): Sender (Host A) không thể approve request của mình")
        void whenSenderTriesToApproveRequest_thenAopDeniesAccess() throws Exception {
            // sampleRequest đã được tạo trong @BeforeEach, chúng ta có thể dùng nó
            ApproveRequestDTO approveDTO = new ApproveRequestDTO("Self-approve?");

            mockMvc.perform(
                            put("/api/requests/{requestId}/approve", sampleRequest.getRequestId())
                                    .with(user(hostA.getCustomer().getUser().getAccount().getEmail()).roles(Role.CUSTOMER.name()))
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(approveDTO))
                                    .sessionAttr("ACCOUNT_ID", hostA.getCustomer().getUser().getAccount().getAccountId())
                    )
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("INT-04 (AOP @RequireRequestReceiver - Chéo): Dept D2 không thể approve request của Dept D1")
        void whenWrongReceiverTriesToApproveRequest_thenAopDeniesAccess() throws Exception {
            ApproveRequestDTO approveDTO = new ApproveRequestDTO("Wrong dept");

            mockMvc.perform(
                            put("/api/requests/{requestId}/approve", sampleRequest.getRequestId())
                                    .with(user(deptD2.getUser().getAccount().getEmail()).roles(Role.DEPARTMENT.name()))
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(approveDTO))
                                    .sessionAttr("ACCOUNT_ID", deptD2.getUser().getAccount().getAccountId())
                    )
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("INT-05 (Edge Case - State): Approve request đã được approve ném ra 400 Bad Request")
        void whenApprovingAlreadyApprovedRequest_thenServiceThrowsException() throws Exception {
            // Given: Cập nhật sampleRequest thành APPROVED
            sampleRequest.setStatus(RequestStatus.APPROVED);
            requestRepo.save(sampleRequest);

            ApproveRequestDTO approveDTO = new ApproveRequestDTO("Approve again");

            mockMvc.perform(
                            put("/api/requests/{requestId}/approve", sampleRequest.getRequestId())
                                    .with(user(deptD1.getUser().getAccount().getEmail()).roles(Role.DEPARTMENT.name()))
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(approveDTO))
                                    .sessionAttr("ACCOUNT_ID", deptD1.getUser().getAccount().getAccountId())
                    )
                    .andExpect(status().isBadRequest());
        }
    }

    // ==========================================================
    // CÁC TEST CASE MỚI ĐỂ COVER REJECT VÀ CREATE JSON
    // ==========================================================

    @Nested
    @DisplayName("Feature: Coverage cho PUT (Reject) và POST (Json)")
    class PutAndPostCoverageTests {

        @Test
        @DisplayName("COVERAGE (Reject - Happy Path): Receiver từ chối request thành công")
        void whenReceiverRejectsRequest_thenSucceeds() throws Exception {
            ApproveRequestDTO rejectDTO = new ApproveRequestDTO("Rejected by Dept D1");

            mockMvc.perform(
                            put("/api/requests/{requestId}/reject", sampleRequest.getRequestId())
                                    .with(user(deptD1.getUser().getAccount().getEmail()).roles(Role.DEPARTMENT.name())) // Login với receiver
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(rejectDTO))
                                    .sessionAttr("ACCOUNT_ID", deptD1.getUser().getAccount().getAccountId())
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("REJECTED"));

            Request rejectedRequest = requestRepo.findById(sampleRequest.getRequestId()).orElseThrow();
            assertThat(rejectedRequest.getStatus()).isEqualTo(RequestStatus.REJECTED);
        }

        @Test
        @DisplayName("COVERAGE (Reject - AOP): Sender không thể từ chối request")
        void whenSenderRejectsRequest_thenForbidden() throws Exception {
            ApproveRequestDTO rejectDTO = new ApproveRequestDTO("Rejected by sender");

            mockMvc.perform(
                            put("/api/requests/{requestId}/reject", sampleRequest.getRequestId())
                                    .with(user(hostA.getCustomer().getUser().getAccount().getEmail()).roles(Role.CUSTOMER.name())) // Login với sender
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(rejectDTO))
                                    .sessionAttr("ACCOUNT_ID", hostA.getCustomer().getUser().getAccount().getAccountId())
                    )
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("COVERAGE (Reject - Logic): Từ chối request đã xử lý ném ra 400 Bad Request")
        void whenRejectingAlreadyProcessedRequest_thenBadRequest() throws Exception {
            // Given
            sampleRequest.setStatus(RequestStatus.APPROVED);
            requestRepo.save(sampleRequest);
            ApproveRequestDTO rejectDTO = new ApproveRequestDTO("Reject again");

            // When & Then
            mockMvc.perform(
                            put("/api/requests/{requestId}/reject", sampleRequest.getRequestId())
                                    .with(user(deptD1.getUser().getAccount().getEmail()).roles(Role.DEPARTMENT.name())) // Login với receiver
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(rejectDTO))
                                    .sessionAttr("ACCOUNT_ID", deptD1.getUser().getAccount().getAccountId())
                    )
                    .andExpect(status().isBadRequest());
        }

        // === SỬA LỖI 1: ĐỔI TÊN TEST VÀ MONG ĐỢI 400 ===
        @Test
        @DisplayName("COVERAGE (createRequestJson - Bad Request): Ném lỗi 400 khi Event ID là null (DB constraint)")
        void whenCreateRequestJsonWithNullEvent_thenBadRequest() throws Exception {
            CreateRequestDTO jsonDTO = CreateRequestDTO.builder()
                    .senderId(hostA.getCustomer().getUser().getAccount().getAccountId())
                    .receiverId(deptD1.getUser().getAccount().getAccountId())
                    .type(RequestType.OTHER)
                    .eventId(null) // <-- Nguyên nhân lỗi
                    .message("Test JSON request")
                    .build();

            mockMvc.perform(
                            post("/api/requests") // Không có multipart
                                    .with(user(hostA.getCustomer().getUser().getAccount().getEmail()).roles(Role.CUSTOMER.name()))
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(jsonDTO))
                    )
                    // Lỗi DB -> Service ném exception -> Controller trả về 400, không phải 201
                    .andExpect(status().isBadRequest());
        }

        // === SỬA LỖI 1: THÊM TEST HAPPY PATH MỚI ===
        @Test
        @DisplayName("COVERAGE (createRequestJson - Happy Path): Tạo request bằng JSON (với EventID) thành công")
        void whenCreateRequestJsonWithEvent_thenSucceeds() throws Exception {
            CreateRequestDTO jsonDTO = CreateRequestDTO.builder()
                    .senderId(hostA.getCustomer().getUser().getAccount().getAccountId())
                    .receiverId(deptD1.getUser().getAccount().getAccountId())
                    .type(RequestType.EVENT_APPROVAL)
                    .eventId(eventE1.getId()) // <-- Cung cấp Event ID
                    .message("Test JSON request with Event")
                    .build();

            mockMvc.perform(
                            post("/api/requests")
                                    .with(user(hostA.getCustomer().getUser().getAccount().getEmail()).roles(Role.CUSTOMER.name()))
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(jsonDTO))
                    )
                    .andExpect(status().isCreated()) // Mong đợi 201
                    .andExpect(jsonPath("$.message").value("Test JSON request with Event"))
                    .andExpect(jsonPath("$.eventId").value(eventE1.getId()));
        }

        @Test
        @DisplayName("COVERAGE (createRequestJson - Bad Request): Ném lỗi khi logic service thất bại (Sender không tồn tại)")
        void whenCreateRequestJsonFails_thenBadRequest() throws Exception {
            CreateRequestDTO badDTO = CreateRequestDTO.builder()
                    .senderId(999L) // Sender không tồn tại
                    .receiverId(deptD1.getUser().getAccount().getAccountId())
                    .eventId(eventE1.getId()) // Phải cung cấp eventId
                    .type(RequestType.OTHER)
                    .build();

            mockMvc.perform(
                            post("/api/requests")
                                    .with(user("tempUser").roles("CUSTOMER")) // Đăng nhập với user bất kỳ để qua security
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(badDTO))
                    )
                    .andExpect(status().isBadRequest());
        }
    }

    // ==========================================================
    // CÁC TEST CASE MỚI ĐỂ COVER TOÀN BỘ ENDPOINT GET
    // ==========================================================

    @Nested
    @DisplayName("Feature: Coverage cho GET Endpoints")
    class GetEndpointsCoverageTests {

        @Test
        @DisplayName("COVERAGE (getRequestById - Happy Path): Lấy request bằng ID thành công")
        void whenGetRequestById_thenSucceeds() throws Exception {
            mockMvc.perform(
                            get("/api/requests/{requestId}", sampleRequest.getRequestId())
                                    .with(user("anyUser").roles("CUSTOMER")) // GET request thường chỉ cần xác thực
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.requestId").value(sampleRequest.getRequestId()));
        }

        @Test
        @DisplayName("COVERAGE (getRequestById - Not Found): Trả về 404 khi không tìm thấy")
        void whenGetRequestByIdNotFound_thenNotFound() throws Exception {
            mockMvc.perform(
                            get("/api/requests/99999")
                                    .with(user("anyUser").roles("CUSTOMER"))
                    )
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("COVERAGE (getAllRequests - Branch 1: No Params): Lấy tất cả request")
        void whenGetAllRequestsNoParams_thenSucceeds() throws Exception {
            mockMvc.perform(
                            get("/api/requests")
                                    .with(user("anyUser").roles("CUSTOMER"))
                    )
                    .andExpect(status().isOk())
                    // === SỬA LỖI 3: Kiểm tra sự tồn tại, không phải vị trí [0] ===
                    .andExpect(jsonPath("$.[*].requestId").value(hasItem(sampleRequest.getRequestId().intValue())));
        }

        @Test
        @DisplayName("COVERAGE (getAllRequests - Branch 2: Status Only): Lấy request theo Status")
        void whenGetAllRequestsByStatus_thenSucceeds() throws Exception {
            mockMvc.perform(
                            get("/api/requests")
                                    .param("status", RequestStatus.PENDING.name())
                                    .with(user("anyUser").roles("CUSTOMER"))
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].status").value("PENDING"));
        }

        @Test
        @DisplayName("COVERAGE (getAllRequests - Branch 3: Type Only): Lấy request theo Type")
        void whenGetAllRequestsByType_thenSucceeds() throws Exception {
            mockMvc.perform(
                            get("/api/requests")
                                    .param("type", RequestType.EVENT_APPROVAL.name())
                                    .with(user("anyUser").roles("CUSTOMER"))
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].type").value("EVENT_APPROVAL"));
        }

        @Test
        @DisplayName("COVERAGE (getRequestsPaginated): Lấy request phân trang")
        void whenGetRequestsPaginated_thenSucceeds() throws Exception {
            mockMvc.perform(
                            get("/api/requests/paginated")
                                    .param("page", "0")
                                    .param("size", "5")
                                    .with(user("anyUser").roles("CUSTOMER"))
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    // === SỬA LỖI 2: Kiểm tra sự tồn tại, không phải tổng số ===
                    .andExpect(jsonPath("$.content.[*].requestId").value(hasItem(sampleRequest.getRequestId().intValue())));
        }

        @Test
        @DisplayName("COVERAGE (getRequestsBySender): Lấy request theo Sender")
        void whenGetRequestsBySender_thenSucceeds() throws Exception {
            mockMvc.perform(
                            get("/api/requests/sender/{senderId}", hostA.getCustomer().getUser().getAccount().getAccountId())
                                    .with(user("anyUser").roles("CUSTOMER"))
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].senderId").value(hostA.getCustomer().getUser().getAccount().getAccountId()));
        }

        @Test
        @DisplayName("COVERAGE (getRequestsByReceiver): Lấy request theo Receiver")
        void whenGetRequestsByReceiver_thenSucceeds() throws Exception {
            mockMvc.perform(
                            get("/api/requests/receiver/{receiverId}", deptD1.getUser().getAccount().getAccountId())
                                    .with(user("anyUser").roles("CUSTOMER"))
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].receiverId").value(deptD1.getUser().getAccount().getAccountId()));
        }

        @Test
        @DisplayName("COVERAGE (getRequestsByEvent): Lấy request theo Event")
        void whenGetRequestsByEvent_thenSucceeds() throws Exception {
            mockMvc.perform(
                            get("/api/requests/event/{eventId}", eventE1.getId())
                                    .with(user("anyUser").roles("CUSTOMER"))
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].eventId").value(eventE1.getId()));
        }

        @Test
        @DisplayName("COVERAGE (showRequestForm): Lấy data cho form")
        void whenShowRequestForm_thenSucceeds() throws Exception {
            mockMvc.perform(
                            get("/api/requests/form")
                                    .param("eventId", eventE1.getId().toString())
                                    .with(user("anyUser").roles("CUSTOMER"))
                    )
                    .andExpect(status().isOk()); // Endpoint này trả về view "fragments/request-form"
        }
    }
}

