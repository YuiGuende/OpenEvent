package com.group02.openevent.service.impl;

import com.group02.openevent.dto.requestApproveEvent.ApproveRequestDTO;
import com.group02.openevent.dto.requestApproveEvent.CreateRequestDTO;
import com.group02.openevent.dto.requestApproveEvent.RequestDTO;
import com.group02.openevent.model.account.Account;
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
@ExtendWith(MockitoExtension.class) // Chỉ khởi tạo Mockito, không chạy Spring
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
    private IDepartmentRepo departmentRepository; // Cần mock dependency này

    @InjectMocks // Tiêm các mock trên vào SUT
    private RequestServiceImpl requestService; // System Under Test (SUT)

    @Captor
    private ArgumentCaptor<Request> requestCaptor;

    private Account sender;
    private Account receiver;
    private Event event;
    private CreateRequestDTO createRequestDTO;

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
    }

    @Nested
    @DisplayName("Feature: createRequestWithFile")
    class CreateRequestWithFileTests {

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
    @DisplayName("Feature: approveRequest")
    class ApproveRequestTests {

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
            verify(requestRepo, never()).findByStatus(any(), any());
            verify(requestRepo, never()).findByType(any(), any());
            verify(requestRepo, never()).findAll(pageable);
        }

        @Test
        @DisplayName("UNIT-10 (Branch: Status only): Gọi đúng repo method")
        void whenListWithStatusOnly_thenCallFindByStatus() {
            // Given
            when(requestRepo.findByStatus(any(), any())).thenReturn(emptyPage);
            // When
            requestService.listRequests(RequestStatus.PENDING, null, pageable);
            // Then
            verify(requestRepo, never()).findByStatusAndType(any(), any(), any());
            verify(requestRepo, times(1)).findByStatus(RequestStatus.PENDING, pageable);
            verify(requestRepo, never()).findByType(any(), any());
            verify(requestRepo, never()).findAll(pageable);
        }

        @Test
        @DisplayName("UNIT-11 (Branch: Type only): Gọi đúng repo method")
        void whenListWithTypeOnly_thenCallFindByType() {
            // Given
            when(requestRepo.findByType(any(), any())).thenReturn(emptyPage);
            // When
            requestService.listRequests(null, RequestType.EVENT_APPROVAL, pageable);
            // Then
            verify(requestRepo, never()).findByStatusAndType(any(), any(), any());
            verify(requestRepo, never()).findByStatus(any(), any());
            verify(requestRepo, times(1)).findByType(RequestType.EVENT_APPROVAL, pageable);
            verify(requestRepo, never()).findAll(pageable);
        }

        @Test
        @DisplayName("UNIT-12 (Branch: Default): Gọi đúng repo method")
        void whenListWithNulls_thenCallFindAll() {
            // Given
            when(requestRepo.findAll(pageable)).thenReturn(emptyPage);
            // When
            requestService.listRequests(null, null, pageable);
            // Then
            verify(requestRepo, never()).findByStatusAndType(any(), any(), any());
            verify(requestRepo, never()).findByStatus(any(), any());
            verify(requestRepo, never()).findByType(any(), any());
            verify(requestRepo, times(1)).findAll(pageable);
        }
    }

    @Nested
    @DisplayName("Feature: convertToDTO (Mapper Logic)")
    class ConvertToDTOTests {

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
            assertThat(dto.getReceiverId()).isNull();
            assertThat(dto.getReceiverName()).isNull();
            assertThat(dto.getEventId()).isNull();
            assertThat(dto.getEventTitle()).isNull();
            assertThat(dto.getOrderId()).isNull();
        }
    }
}