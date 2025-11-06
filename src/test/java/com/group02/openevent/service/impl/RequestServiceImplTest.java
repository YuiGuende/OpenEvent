package com.group02.openevent.service.impl;

import com.group02.openevent.dto.notification.RequestFormDTO; // THÊM IMPORT
import com.group02.openevent.dto.requestApproveEvent.ApproveRequestDTO;
import com.group02.openevent.dto.requestApproveEvent.CreateRequestDTO;
import com.group02.openevent.dto.requestApproveEvent.RequestDTO;
import com.group02.openevent.model.account.Account;
import com.group02.openevent.model.department.Department;
import com.group02.openevent.model.user.User;
import com.group02.openevent.model.enums.EventStatus;
import com.group02.openevent.model.event.Event;
import com.group02.openevent.model.request.Request;
import com.group02.openevent.model.request.RequestStatus;
import com.group02.openevent.model.request.RequestType;
import com.group02.openevent.repository.IAccountRepo;
import com.group02.openevent.repository.IDepartmentRepo;
import com.group02.openevent.repository.IRequestRepo;
import com.group02.openevent.service.EventService;
import com.group02.openevent.util.CloudinaryUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("Unit Test cho RequestServiceImpl")
@ExtendWith(MockitoExtension.class)
class RequestServiceImplTest {

    @Mock
    private IRequestRepo requestRepo;
    @Mock
    private EventService eventService;
    @Mock
    private CloudinaryUtil cloudinaryUtil;
    @Mock
    private IAccountRepo accountRepo;
    @Mock
    private IDepartmentRepo departmentRepository;

    @InjectMocks
    private RequestServiceImpl requestService;

    @Captor
    private ArgumentCaptor<Request> requestCaptor;

    private Account sender;
    private Account receiver;
    private Event event;
    private CreateRequestDTO createRequestDTO;
    private Request sampleRequest;

    @BeforeEach
    void setUp() {
        sender = new Account();
        sender.setAccountId(1L);
        sender.setEmail("sender@mail.com");

        receiver = new Account();
        receiver.setAccountId(2L);
        receiver.setEmail("receiver@mail.com");

        event = new Event();
        event.setId(10L);
        event.setTitle("Test Event");

        createRequestDTO = CreateRequestDTO.builder()
                .senderId(1L)
                .receiverId(2L)
                .eventId(10L)
                .type(RequestType.EVENT_APPROVAL)
                .message("Test message")
                .build();

        sampleRequest = Request.builder()
                .requestId(1L)
                .sender(sender)
                .receiver(receiver)
                .event(event)
                .status(RequestStatus.PENDING)
                .type(RequestType.EVENT_APPROVAL)
                .createdAt(LocalDateTime.now())
                .build();
    }

    // ==========================================================
    // CÁC TEST CASE HIỆN TẠI CỦA BẠN (ĐANG HOẠT ĐỘNG TỐT)
    // ==========================================================

    @Nested
    @DisplayName("Feature: createRequestWithFile")
    class CreateRequestWithFileTests {
        // ... (Giữ nguyên toàn bộ test case của bạn ở đây) ...
        // (Bao gồm whenCreateRequestWithFile_thenUploadSucceedsAndRequestSaved, ...)
        @Mock
        private MultipartFile file;

        @Test
        @DisplayName("UNIT-01 (Happy Path): Tạo request với file upload thành công")
        void whenCreateRequestWithFile_thenUploadSucceedsAndRequestSaved() throws IOException {
            // Given (Điều kiện - Mock Setup)
            when(file.isEmpty()).thenReturn(false);
            when(cloudinaryUtil.uploadFile(file)).thenReturn("http://cloudinary.com/test-url");
            when(accountRepo.findById(1L)).thenReturn(Optional.of(sender));
            when(accountRepo.findById(2L)).thenReturn(Optional.of(receiver));
            when(eventService.getEventById(10L)).thenReturn(Optional.of(event));
            when(requestRepo.save(any(Request.class))).thenAnswer(invocation -> invocation.getArgument(0));
            // When (Hành động)
            requestService.createRequestWithFile(createRequestDTO, file);

            // Then (Kết quả - Assert & Verify)
            verify(cloudinaryUtil, times(1)).uploadFile(file);
            verify(requestRepo, times(1)).save(requestCaptor.capture());

            Request savedRequest = requestCaptor.getValue();
            assertThat(savedRequest.getFileURL()).isEqualTo("http://cloudinary.com/test-url");
            assertThat(savedRequest.getSender()).isEqualTo(sender);
            assertThat(savedRequest.getReceiver()).isEqualTo(receiver);
            assertThat(savedRequest.getEvent()).isEqualTo(event);
            assertThat(savedRequest.getStatus()).isEqualTo(RequestStatus.PENDING);
        }

        @Test
        @DisplayName("UNIT-02 (No File): Tạo request với file là null")
        void whenCreateRequestWithNullFile_thenUploadSkippedAndRequestSaved() throws IOException {
            // Given
            when(accountRepo.findById(1L)).thenReturn(Optional.of(sender));
            when(accountRepo.findById(2L)).thenReturn(Optional.of(receiver));
            when(eventService.getEventById(10L)).thenReturn(Optional.of(event));
            when(requestRepo.save(any(Request.class))).thenAnswer(invocation -> invocation.getArgument(0));
            // When
            requestService.createRequestWithFile(createRequestDTO, null); // file là null

            // Then
            verify(cloudinaryUtil, never()).uploadFile(any());
            verify(requestRepo, times(1)).save(requestCaptor.capture());
            assertThat(requestCaptor.getValue().getFileURL()).isNull();
        }

        @Test
        @DisplayName("UNIT-03 (Empty File): Tạo request với file rỗng (isEmpty)")
        void whenCreateRequestWithEmptyFile_thenUploadSkippedAndRequestSaved() throws IOException {
            // Given
            when(file.isEmpty()).thenReturn(true); // file rỗng
            when(accountRepo.findById(1L)).thenReturn(Optional.of(sender));
            when(accountRepo.findById(2L)).thenReturn(Optional.of(receiver));
            when(eventService.getEventById(10L)).thenReturn(Optional.of(event));
            when(requestRepo.save(any(Request.class))).thenAnswer(invocation -> invocation.getArgument(0));
            // When
            requestService.createRequestWithFile(createRequestDTO, file);

            // Then
            verify(cloudinaryUtil, never()).uploadFile(any());
            verify(requestRepo, times(1)).save(requestCaptor.capture());
            assertThat(requestCaptor.getValue().getFileURL()).isNull();
        }

        @Test
        @DisplayName("UNIT-04 (Upload Fail): Ném ra RuntimeException khi upload file thất bại")
        void whenUploadFails_thenThrowRuntimeException() throws IOException {
            // Given
            when(file.isEmpty()).thenReturn(false);
            when(cloudinaryUtil.uploadFile(file)).thenThrow(new IOException("Upload Failed"));

            // When & Then
            assertThatThrownBy(() -> requestService.createRequestWithFile(createRequestDTO, file))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Failed to upload file: Upload Failed");

            verify(requestRepo, never()).save(any());
        }

        @Test
        @DisplayName("UNIT-05 (Data Fail): Ném ra RuntimeException khi Sender không tìm thấy")
        void whenSenderNotFound_thenThrowRuntimeException() throws IOException {
            // Given
            when(file.isEmpty()).thenReturn(false);
            when(cloudinaryUtil.uploadFile(any())).thenReturn("http://url.com");
            when(accountRepo.findById(1L)).thenReturn(Optional.empty()); // Sender không tìm thấy

            // When & Then
            assertThatThrownBy(() -> requestService.createRequestWithFile(createRequestDTO, file))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Sender not found");

            verify(requestRepo, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Feature: approveRequest (DTO)")
    class ApproveRequestTests {
        // ... (Giữ nguyên toàn bộ test case của bạn ở đây) ...
        private final Long requestId = 1L;
        private final ApproveRequestDTO approveDTO = new ApproveRequestDTO("Looks good!");

        @Test
        @DisplayName("UNIT-06 (Happy Path): Approve request EVENT_APPROVAL thành công")
        void whenApproveEventApprovalRequest_thenSetStatusApprovedAndUpdateEvent() {
            // Given
            Request pendingRequest = Request.builder()
                    .requestId(requestId)
                    .status(RequestStatus.PENDING)
                    .type(RequestType.EVENT_APPROVAL)
                    .event(event) // eventId 10L
                    .build();
            when(requestRepo.findById(requestId)).thenReturn(Optional.of(pendingRequest));
            when(requestRepo.save(any(Request.class))).thenAnswer(invocation -> invocation.getArgument(0));
            // When
            requestService.approveRequest(requestId, approveDTO);

            // Then
            verify(requestRepo, times(1)).save(requestCaptor.capture());
            Request savedRequest = requestCaptor.getValue();

            assertThat(savedRequest.getStatus()).isEqualTo(RequestStatus.APPROVED);
            assertThat(savedRequest.getResponseMessage()).isEqualTo("Looks good!");
            assertThat(savedRequest.getUpdatedAt()).isNotNull();

            // Verify side effect
            verify(eventService, times(1)).updateEventStatus(10L, EventStatus.PUBLIC);
        }

        @Test
        @DisplayName("UNIT-07 (Branch: Not PENDING): Ném ra RuntimeException khi request đã được xử lý")
        void whenRequestAlreadyProcessed_thenThrowRuntimeException() {
            // Given
            Request approvedRequest = Request.builder()
                    .requestId(requestId)
                    .status(RequestStatus.APPROVED) // Đã được approve
                    .build();
            when(requestRepo.findById(requestId)).thenReturn(Optional.of(approvedRequest));

            // When & Then
            assertThatThrownBy(() -> requestService.approveRequest(requestId, approveDTO))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Request has already been processed");

            verify(requestRepo, never()).save(any());
            verify(eventService, never()).updateEventStatus(any(), any());
        }

        @Test
        @DisplayName("UNIT-08 (Branch: Not Event Approval): Approve request (ví dụ: REFUND) thành công")
        void whenApproveOtherRequestType_thenSetStatusApprovedAndNoEventUpdate() {
            // Given
            Request pendingRequest = Request.builder()
                    .requestId(requestId)
                    .status(RequestStatus.PENDING)
                    .type(RequestType.REFUND_TICKET) // Loại request khác
                    .event(event)
                    .build();
            when(requestRepo.findById(requestId)).thenReturn(Optional.of(pendingRequest));
            when(requestRepo.save(any(Request.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            requestService.approveRequest(requestId, approveDTO);

            // Then
            verify(requestRepo, times(1)).save(requestCaptor.capture());
            Request savedRequest = requestCaptor.getValue();

            assertThat(savedRequest.getStatus()).isEqualTo(RequestStatus.APPROVED);

            // Verify side effect (KHÔNG update event)
            verify(eventService, never()).updateEventStatus(any(), any());
        }
    }

    @Nested
    @DisplayName("Feature: listRequests (Pagination Logic)")
    class ListRequestsTests {
        // ... (Giữ nguyên toàn bộ test case của bạn ở đây) ...
        private final Pageable pageable = PageRequest.of(0, 10);
        private final Page<Request> emptyPage = new PageImpl<>(List.of());

        @Test
        @DisplayName("UNIT-09 (Branch: Status & Type): Gọi đúng repo method")
        void whenListWithStatusAndType_thenCallFindByStatusAndType() {
            // Given
            when(requestRepo.findByStatusAndType(any(), any(), any())).thenReturn(emptyPage);
            // When
            requestService.listRequests(RequestStatus.PENDING, RequestType.EVENT_APPROVAL, pageable);
            // Then
            verify(requestRepo, times(1)).findByStatusAndType(RequestStatus.PENDING, RequestType.EVENT_APPROVAL, pageable);
        }

        // ... (Các test khác: UNIT-10, 11, 12) ...
        @Test
        @DisplayName("UNIT-10 (Branch: Status only): Gọi đúng repo method")
        void whenListWithStatusOnly_thenCallFindByStatus() {
            // Given
            when(requestRepo.findByStatus(any(), any())).thenReturn(emptyPage);
            // When
            requestService.listRequests(RequestStatus.PENDING, null, pageable);
            // Then
            verify(requestRepo, times(1)).findByStatus(RequestStatus.PENDING, pageable);
        }

        @Test
        @DisplayName("UNIT-11 (Branch: Type only): Gọi đúng repo method")
        void whenListWithTypeOnly_thenCallFindByType() {
            // Given
            when(requestRepo.findByType(any(), any())).thenReturn(emptyPage);
            // When
            requestService.listRequests(null, RequestType.EVENT_APPROVAL, pageable);
            // Then
            verify(requestRepo, times(1)).findByType(RequestType.EVENT_APPROVAL, pageable);
        }

        @Test
        @DisplayName("UNIT-12 (Branch: Default): Gọi đúng repo method")
        void whenListWithNulls_thenCallFindAll() {
            // Given
            when(requestRepo.findAll(pageable)).thenReturn(emptyPage);
            // When
            requestService.listRequests(null, null, pageable);
            // Then
            verify(requestRepo, times(1)).findAll(pageable);
        }
    }

    @Nested
    @DisplayName("Feature: convertToDTO (Mapper Logic)")
    class ConvertToDTOTests {
        // ... (Giữ nguyên toàn bộ test case của bạn ở đây) ...
        @Test
        @DisplayName("UNIT-13 (Null Checks): Xử lý an toàn các quan hệ (relation) bị null")
        void whenRelationsAreNull_thenConvertToDTONotThrowsNullPointerException() {
            // Given
            Request request = new Request(); // sender, receiver, event, order đều là null
            request.setRequestId(1L);
            request.setStatus(RequestStatus.PENDING);
            request.setType(RequestType.OTHER);
            request.setCreatedAt(LocalDateTime.now());

            // When
            RequestDTO dto = requestService.convertToDTO(request);

            // Then (Không ném NullPointerException)
            assertThat(dto.getSenderId()).isNull();
            assertThat(dto.getSenderName()).isNull();
            // ... (các assertion khác) ...
        }
    }

    // ==========================================================
    // CÁC TEST CASE MỚI ĐỂ ĐẠT >95% COVERAGE
    // ==========================================================

    @Nested
    @DisplayName("Feature: rejectRequest (DTO)")
    class RejectRequestTests {

        private final Long requestId = 1L;
        private final ApproveRequestDTO rejectDTO = new ApproveRequestDTO("Rejected");

        @Test
        @DisplayName("Happy Path: Từ chối request thành công")
        void whenRejectRequest_thenSetStatusRejected() {
            // Given
            when(requestRepo.findById(requestId)).thenReturn(Optional.of(sampleRequest));
            when(requestRepo.save(any(Request.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            RequestDTO resultDTO = requestService.rejectRequest(requestId, rejectDTO);

            // Then
            verify(requestRepo, times(1)).save(requestCaptor.capture());
            Request savedRequest = requestCaptor.getValue();

            assertThat(savedRequest.getStatus()).isEqualTo(RequestStatus.REJECTED);
            assertThat(savedRequest.getResponseMessage()).isEqualTo("Rejected");
            assertThat(savedRequest.getUpdatedAt()).isNotNull();
            assertThat(resultDTO.getStatus()).isEqualTo(RequestStatus.REJECTED);

            // Đảm bảo không có side effect
            verify(eventService, never()).updateEventStatus(any(), any());
        }

        @Test
        @DisplayName("Edge Case (Not PENDING): Ném lỗi khi từ chối request đã xử lý")
        void whenRejectAlreadyProcessed_thenThrowException() {
            // Given
            sampleRequest.setStatus(RequestStatus.REJECTED);
            when(requestRepo.findById(requestId)).thenReturn(Optional.of(sampleRequest));

            // When & Then
            assertThatThrownBy(() -> requestService.rejectRequest(requestId, rejectDTO))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Request has already been processed");
            verify(requestRepo, never()).save(any());
        }

        @Test
        @DisplayName("Edge Case (Not Found): Ném lỗi khi không tìm thấy request")
        void whenRejectNotFound_thenThrowException() {
            // Given
            when(requestRepo.findById(requestId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> requestService.rejectRequest(requestId, rejectDTO))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Request not found");
        }
    }

    @Nested
    @DisplayName("Feature: createRequest (No File)")
    class CreateRequestTests {

        @Test
        @DisplayName("Happy Path: Tạo request (không file) thành công")
        void whenCreateRequestNoFile_thenRequestSaved() {
            // Given
            when(accountRepo.findById(1L)).thenReturn(Optional.of(sender));
            when(accountRepo.findById(2L)).thenReturn(Optional.of(receiver));
            when(eventService.getEventById(10L)).thenReturn(Optional.of(event));
            when(requestRepo.save(any(Request.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            RequestDTO resultDTO = requestService.createRequest(createRequestDTO);

            // Then
            verify(requestRepo, times(1)).save(requestCaptor.capture());
            Request savedRequest = requestCaptor.getValue();

            assertThat(savedRequest.getEvent()).isEqualTo(event);
            assertThat(savedRequest.getSender()).isEqualTo(sender);
            assertThat(resultDTO.getEventId()).isEqualTo(10L);
        }

        @Test
        @DisplayName("Happy Path (No Event): Tạo request (không file, không event) thành công")
        void whenCreateRequestNoFileNoEvent_thenRequestSaved() {
            // Given
            createRequestDTO.setEventId(null);
            when(accountRepo.findById(1L)).thenReturn(Optional.of(sender));
            when(accountRepo.findById(2L)).thenReturn(Optional.of(receiver));
            when(requestRepo.save(any(Request.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            RequestDTO resultDTO = requestService.createRequest(createRequestDTO);

            // Then
            verify(requestRepo, times(1)).save(requestCaptor.capture());
            Request savedRequest = requestCaptor.getValue();

            assertThat(savedRequest.getEvent()).isNull();
            verify(eventService, never()).getEventById(any());
            assertThat(resultDTO.getEventId()).isNull();
        }

        @Test
        @DisplayName("Edge Case (Event Not Found): Ném lỗi khi event không tìm thấy")
        void whenCreateRequestEventNotFound_thenThrowException() {
            // Given
            when(accountRepo.findById(1L)).thenReturn(Optional.of(sender));
            when(accountRepo.findById(2L)).thenReturn(Optional.of(receiver));
            when(eventService.getEventById(10L)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> requestService.createRequest(createRequestDTO))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Event not found");
            verify(requestRepo, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Feature: getRequestFormData")
    class GetRequestFormDataTests {

        @Test
        @DisplayName("Happy Path: Lấy data cho form thành công")
        void whenGetRequestFormData_thenSucceed() throws Exception {
            // Given
            Account account = new Account();
            account.setAccountId(100L);
            User user = new User();
            user.setAccount(account);
            user.setUserId(100L);
            Department dept1 = new Department();
            dept1.setUser(user);
            dept1.setDepartmentName("Dept 1");
            when(departmentRepository.findAll()).thenReturn(List.of(dept1));
            when(eventService.getEventById(10L)).thenReturn(Optional.of(event));

            // When
            RequestFormDTO formData = requestService.getRequestFormData(10L);

            // Then
            assertThat(formData.getEventId()).isEqualTo(10L);
            assertThat(formData.getEventName()).isEqualTo("Test Event");
            assertThat(formData.getDepartments()).hasSize(1);
            assertThat(formData.getDepartments().get(0).getName()).isEqualTo("Dept 1");
        }

        @Test
        @DisplayName("Edge Case (Event Not Found): Ném lỗi khi event không tìm thấy")
        void whenGetFormDataEventNotFound_thenThrowException() {
            // Given
            when(departmentRepository.findAll()).thenReturn(List.of());
            when(eventService.getEventById(10L)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> requestService.getRequestFormData(10L))
                    .isInstanceOf(Exception.class)
                    .hasMessage("event not found!");
        }
    }

    @Nested
    @DisplayName("Feature: Simple Getters (List-based)")
    class SimpleListGetterTests {

        @Test
        @DisplayName("getAllRequests: Trả về danh sách DTO (Cover lambda)")
        void whenGetAllRequests_thenReturnDtoList() {
            // Given
            when(requestRepo.findAll()).thenReturn(List.of(sampleRequest));
            // When
            List<RequestDTO> results = requestService.getAllRequests();
            // Then
            assertThat(results).hasSize(1);
            assertThat(results.get(0).getRequestId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("getRequestsByStatus: Trả về danh sách DTO (Cover lambda)")
        void whenGetRequestsByStatus_thenReturnDtoList() {
            // Given
            when(requestRepo.findByStatus(RequestStatus.PENDING)).thenReturn(List.of(sampleRequest));
            // When
            List<RequestDTO> results = requestService.getRequestsByStatus(RequestStatus.PENDING);
            // Then
            assertThat(results).hasSize(1);
            verify(requestRepo, times(1)).findByStatus(RequestStatus.PENDING);
        }

        @Test
        @DisplayName("getRequestsByType: Trả về danh sách DTO (Cover lambda)")
        void whenGetRequestsByType_thenReturnDtoList() {
            // Given
            when(requestRepo.findByType(RequestType.EVENT_APPROVAL)).thenReturn(List.of(sampleRequest));
            // When
            List<RequestDTO> results = requestService.getRequestsByType(RequestType.EVENT_APPROVAL);
            // Then
            assertThat(results).hasSize(1);
            verify(requestRepo, times(1)).findByType(RequestType.EVENT_APPROVAL);
        }

        @Test
        @DisplayName("getRequestsBySenderId: Trả về danh sách DTO (Cover lambda)")
        void whenGetRequestsBySenderId_thenReturnDtoList() {
            // Given
            when(requestRepo.findBySenderAccountId(1L)).thenReturn(List.of(sampleRequest));
            // When
            List<RequestDTO> results = requestService.getRequestsBySenderId(1L);
            // Then
            assertThat(results).hasSize(1);
            verify(requestRepo, times(1)).findBySenderAccountId(1L);
        }

        @Test
        @DisplayName("getRequestsByReceiverId: Trả về danh sách DTO (Cover lambda)")
        void whenGetRequestsByReceiverId_thenReturnDtoList() {
            // Given
            when(requestRepo.findByReceiver_AccountId(2L)).thenReturn(List.of(sampleRequest));
            // When
            List<RequestDTO> results = requestService.getRequestsByReceiverId(2L);
            // Then
            assertThat(results).hasSize(1);
            verify(requestRepo, times(1)).findByReceiver_AccountId(2L);
        }

        @Test
        @DisplayName("getRequestsByEventId: Trả về danh sách DTO (Cover lambda)")
        void whenGetRequestsByEventId_thenReturnDtoList() {
            // Given
            when(requestRepo.findByEvent_Id(10L)).thenReturn(List.of(sampleRequest));
            // When
            List<RequestDTO> results = requestService.getRequestsByEventId(10L);
            // Then
            assertThat(results).hasSize(1);
            verify(requestRepo, times(1)).findByEvent_Id(10L);
        }
    }

    @Nested
    @DisplayName("Feature: getRequestById (Optional-based)")
    class GetRequestByIdTests {

        @Test
        @DisplayName("Happy Path: Tìm thấy request (Cover lambda)")
        void whenGetRequestByIdFound_thenReturnOptionalDto() {
            // Given
            when(requestRepo.findById(1L)).thenReturn(Optional.of(sampleRequest));
            // When
            Optional<RequestDTO> result = requestService.getRequestById(1L);
            // Then
            assertThat(result).isPresent();
            assertThat(result.get().getRequestId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("Edge Case (Not Found): Không tìm thấy request")
        void whenGetRequestByIdNotFound_thenReturnEmptyOptional() {
            // Given
            when(requestRepo.findById(1L)).thenReturn(Optional.empty());
            // When
            Optional<RequestDTO> result = requestService.getRequestById(1L);
            // Then
            assertThat(result).isNotPresent();
        }
    }

    @Nested
    @DisplayName("Feature: getRequestsByReceiver (Pageable)")
    class GetRequestsByReceiverPageableTests {

        private final Pageable pageable = PageRequest.of(0, 10);

        @Test
        @DisplayName("Branch (Status != null): Gọi đúng repo method")
        void whenGetByReceiverWithStatus_thenCallCorrectRepo() {
            // Given
            Page<Request> page = new PageImpl<>(List.of(sampleRequest));
            when(requestRepo.findByReceiver_AccountIdAndStatus(2L, RequestStatus.PENDING, pageable)).thenReturn(page);

            // When
            Page<Request> results = requestService.getRequestsByReceiver(2L, RequestStatus.PENDING, pageable);

            // Then
            assertThat(results.getTotalElements()).isEqualTo(1);
            verify(requestRepo, times(1)).findByReceiver_AccountIdAndStatus(2L, RequestStatus.PENDING, pageable);
            verify(requestRepo, never()).findByReceiver_AccountId(anyLong(), any(Pageable.class));
        }

        @Test
        @DisplayName("Branch (Status == null): Gọi đúng repo method")
        void whenGetByReceiverWithNullStatus_thenCallCorrectRepo() {
            // Given
            Page<Request> page = new PageImpl<>(List.of(sampleRequest));
            when(requestRepo.findByReceiver_AccountId(2L, pageable)).thenReturn(page);

            // When
            Page<Request> results = requestService.getRequestsByReceiver(2L, null, pageable);

            // Then
            assertThat(results.getTotalElements()).isEqualTo(1);
            verify(requestRepo, never()).findByReceiver_AccountIdAndStatus(anyLong(), any(), any());
            verify(requestRepo, times(1)).findByReceiver_AccountId(2L, pageable);
        }
    }

    @Nested
    @DisplayName("Feature: listRequestsByReceiver (Pageable, DTO)")
    class ListRequestsByReceiverPageableTests {

        @Test
        @DisplayName("Happy Path: Trả về DTO page (Cover lambda)")
        void whenListByReceiver_thenReturnDtoPage() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            Page<Request> page = new PageImpl<>(List.of(sampleRequest), pageable, 1);
            when(requestRepo.findByReceiver_AccountId(2L, pageable)).thenReturn(page);

            // When
            Page<RequestDTO> resultPage = requestService.listRequestsByReceiver(2L, pageable);

            // Then
            assertThat(resultPage.getTotalElements()).isEqualTo(1);
            assertThat(resultPage.getContent().get(0).getRequestId()).isEqualTo(1L);
        }
    }

    @Nested
    @DisplayName("Feature: Overloaded Methods (String message)")
    class OverloadedMethodsTests {

        @Test
        @DisplayName("approveRequest(String): Hoạt động chính xác")
        void whenApproveRequestWithString_thenSavesAndUpdatesEvent() {
            // Given
            when(requestRepo.findById(1L)).thenReturn(Optional.of(sampleRequest));
            when(requestRepo.save(any(Request.class))).thenReturn(sampleRequest);

            // When
            Request result = requestService.approveRequest(1L, "Approved");

            // Then
            verify(requestRepo, times(1)).save(requestCaptor.capture());
            Request savedRequest = requestCaptor.getValue();

            assertThat(savedRequest.getStatus()).isEqualTo(RequestStatus.APPROVED);
            assertThat(savedRequest.getResponseMessage()).isEqualTo("Approved");
            verify(eventService, times(1)).updateEventStatus(10L, EventStatus.PUBLIC);
        }

        @Test
        @DisplayName("rejectRequest(String): Hoạt động chính xác")
        void whenRejectRequestWithString_thenSaves() {
            // Given
            when(requestRepo.findById(1L)).thenReturn(Optional.of(sampleRequest));
            when(requestRepo.save(any(Request.class))).thenReturn(sampleRequest);

            // When
            Request result = requestService.rejectRequest(1L, "Rejected");

            // Then
            verify(requestRepo, times(1)).save(requestCaptor.capture());
            Request savedRequest = requestCaptor.getValue();

            assertThat(savedRequest.getStatus()).isEqualTo(RequestStatus.REJECTED);
            assertThat(savedRequest.getResponseMessage()).isEqualTo("Rejected");
            verify(eventService, never()).updateEventStatus(any(), any());
        }
    }
}
