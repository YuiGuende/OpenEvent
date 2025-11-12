## Decision Table Testing: Duyệt Sự Kiện (Event Approval)

Phạm vi: Dựa trên các test hiện có trong `src/test/java` liên quan đến quy trình phê duyệt sự kiện qua `Request`:
- Service: `RequestServiceImplTest` (approve/reject, create request)
- Integration: `RequestControllerIntegrationTest` (AOP, quyền, HTTP status)
- Bổ trợ: `EventServiceTest` (side-effect cập nhật trạng thái sự kiện)

Các thực thể/điều kiện chính:
- Request status: `PENDING`, `APPROVED`, `REJECTED`
- Request type: `EVENT_APPROVAL`, `OTHER | REFUND_TICKET`
- Actor: `Sender (Host)`, `Receiver (Department)`, `Wrong Receiver`, `Wrong Host`
- Kết quả mong đợi: HTTP Status, trạng thái `Request`, trạng thái `Event`, thông điệp phản hồi

### Bảng 1: Approve Request (EVENT_APPROVAL)

Điều kiện:
- C1: Request tồn tại?
- C2: Request.status = PENDING?
- C3: Request.type = EVENT_APPROVAL?
- C4: Caller là đúng `Receiver`?

Hành động/Kết quả:
- R1: Trả HTTP Status
- R2: Lưu `Request.status = APPROVED`
- R3: Ghi `responseMessage`
- R4: Cập nhật `updatedAt`
- R5: Cập nhật `Event.status = PUBLIC` (chỉ khi type = EVENT_APPROVAL)

| Rule | C1 Exists | C2 Pending | C3 Type=EVENT_APPROVAL | C4 CallerIsReceiver | R1 HTTP | R2 Req=APPROVED | R3 responseMessage | R4 updatedAt | R5 Event=PUBLIC |
|---|---|---|---|---|---|---|---|---|---|
| A1 (Happy Path) | Y | Y | Y | Y | 200 | Y | Y | Y | Y |
| A2 (Processed) | Y | N | any | Y | 400 | N | N | N | N |
| A3 (Wrong Receiver) | Y | Y | any | N | 403 | N | N | N | N |
| A4 (Sender self-approve) | Y | Y | any | N | 403 | N | N | N | N |
| A5 (Other Type) | Y | Y | N | Y | 200 | Y | Y | Y | N |

Mapping test:
- A1: `RequestServiceImplTest#whenApproveEventApprovalRequest_thenSetStatusApprovedAndUpdateEvent`, `RequestControllerIntegrationTest#INT-01`
- A2: `RequestServiceImplTest#whenRequestAlreadyProcessed_thenThrowRuntimeException`, `RequestControllerIntegrationTest#INT-05`
- A3: `RequestControllerIntegrationTest#INT-04`
- A4: `RequestControllerIntegrationTest#INT-03`
- A5: `RequestServiceImplTest#whenApproveOtherRequestType_thenSetStatusApprovedAndNoEventUpdate`


### Bảng 2: Reject Request

Điều kiện:
- C1: Request tồn tại?
- C2: Request.status = PENDING?
- C3: Caller là đúng `Receiver`?

Hành động/Kết quả:
- R1: Trả HTTP Status
- R2: Lưu `Request.status = REJECTED`
- R3: Ghi `responseMessage`
- R4: Cập nhật `updatedAt`
- R5: Không cập nhật Event

| Rule | C1 Exists | C2 Pending | C3 CallerIsReceiver | R1 HTTP | R2 Req=REJECTED | R3 responseMessage | R4 updatedAt | R5 Event Changed |
|---|---|---|---|---|---|---|---|---|
| R1 (Happy Path) | Y | Y | Y | 200 | Y | Y | Y | N |
| R2 (Processed) | Y | N | Y | 400 | N | N | N | N |
| R3 (Sender tries reject) | Y | Y | N | 403 | N | N | N | N |

Mapping test:
- R1: `RequestServiceImplTest#whenRejectRequest_thenSetStatusRejected`, `RequestControllerIntegrationTest#whenReceiverRejectsRequest_thenSucceeds`
- R2: `RequestServiceImplTest#whenRejectAlreadyProcessed_thenThrowException`, `RequestControllerIntegrationTest#whenRejectingAlreadyProcessedRequest_thenBadRequest`
- R3: `RequestControllerIntegrationTest#whenSenderRejectsRequest_thenForbidden`


### Bảng 3: Create Request (Host gửi yêu cầu duyệt sự kiện)

Điều kiện:
- C1: Caller là `Host` của Event?
- C2: `receiverId` là Department hợp lệ?
- C3: `eventId` hợp lệ và tồn tại (khi type yêu cầu)?
- C4: Upload file (Multipart) hợp lệ?

Hành động/Kết quả:
- R1: HTTP Status
- R2: Lưu `Request` với `status = PENDING`
- R3: Lưu `fileURL` khi có file hợp lệ

| Rule | C1 HostIsOwner | C2 ReceiverValid | C3 EventValid | C4 FileValid | R1 HTTP | R2 Save PENDING | R3 fileURL |
|---|---|---|---|---|---|---|---|
| C1 (Happy Path multipart) | Y | Y | Y | Y | 201 | Y | Y |
| C2 (Happy Path JSON) | Y | Y | Y | N/A | 201 | Y | N/A |
| C3 (Wrong Host) | N | Y | Y | any | 403 | N | N |
| C4 (Event Not Found khi yêu cầu) | Y | Y | N | any | 400 | N | N |
| C5 (File null/empty) | Y | Y | Y | N | 201 | Y | N |

Mapping test:
- C1: `RequestControllerIntegrationTest#INT-01`
- C2: `RequestControllerIntegrationTest#whenCreateRequestJsonWithEvent_thenSucceeds`
- C3: `RequestControllerIntegrationTest#INT-02 (AOP @RequireEventHost)`
- C4: `RequestServiceImplTest#whenCreateRequestEventNotFound_thenThrowException` và `RequestControllerIntegrationTest#whenCreateRequestJsonFails_thenBadRequest` (invalid sender) / `whenCreateRequestJsonWithNullEvent_thenBadRequest` (DB/logic fail)
- C5: `RequestServiceImplTest#whenCreateRequestWithNullFile_thenUploadSkippedAndRequestSaved`, `whenCreateRequestWithEmptyFile_thenUploadSkippedAndRequestSaved`


### Bảng 4: Get/List Requests (tham khảo nhánh logic)

Điều kiện:
- C1: Filter Status
- C2: Filter Type

Hành động/Kết quả:
- R1: Gọi đúng repository method
- R2: Trả danh sách/Trang DTO hợp lệ

| Rule | C1 Status | C2 Type | R1 Repo method | R2 Kết quả |
|---|---|---|---|---|
| G1 | null | null | `findAll(pageable)` | 200 + page content |
| G2 | PENDING | null | `findByStatus(status, pageable)` | 200 + list/status |
| G3 | null | EVENT_APPROVAL | `findByType(type, pageable)` | 200 + list/type |
| G4 | PENDING | EVENT_APPROVAL | `findByStatusAndType(status,type,pageable)` | 200 + filtered |

Mapping test:
- `RequestServiceImplTest` UNIT-09..12 và `RequestControllerIntegrationTest` phần GET endpoints


### Ghi chú thiết kế test theo bảng quyết định
- Với AOP: xác thực “đúng receiver” và “host sở hữu event” được phản ánh qua các rule “403 Forbidden” (INT-02/03/04).
- Với trạng thái request: chỉ `PENDING` mới có thể `approve`/`reject`; các trạng thái khác trả lỗi 400.
- Side-effect domain: chỉ `approve` với `type = EVENT_APPROVAL` mới cập nhật `Event.status = PUBLIC`; `reject` không đổi event.
- Tại service: khi `approve/reject` dạng DTO/string đều phải lưu `responseMessage` và `updatedAt`.


### Truy vết nhanh sang test hiện hữu (code references)

```216:241:src/test/java/com/group02/openevent/service/impl/RequestServiceImplTest.java
@Test
@DisplayName("UNIT-06 (Happy Path): Approve request EVENT_APPROVAL thành công")
void whenApproveEventApprovalRequest_thenSetStatusApprovedAndUpdateEvent() {
  // ...
  verify(eventService, times(1)).updateEventStatus(10L, EventStatus.PUBLIC);
}
```

```276:286:src/test/java/com/group02/openevent/service/impl/RequestServiceImplTest.java
// Approve type khác -> không update event
requestService.approveRequest(requestId, approveDTO);
verify(eventService, never()).updateEventStatus(any(), any());
```

```209:225:src/test/java/com/group02/openevent/controller/RequestControllerIntegrationTest.java
// INT-01: Flow tạo -> approve, và event chuyển PUBLIC
mockMvc.perform(put("/api/requests/{requestId}/approve", newRequestId) ...)
      .andExpect(status().isOk());
Event updatedEvent = eventRepo.findById(eventE1.getId()).orElseThrow();
assertThat(updatedEvent.getStatus()).isEqualTo(EventStatus.PUBLIC);
```

```276:293:src/test/java/com/group02/openevent/controller/RequestControllerIntegrationTest.java
// INT-05: Approve request đã APPROVED -> 400 Bad Request
mockMvc.perform(put("/api/requests/{requestId}/approve", sampleRequest.getRequestId()) ...)
      .andExpect(status().isBadRequest());
```

```324:336:src/test/java/com/group02/openevent/controller/RequestControllerIntegrationTest.java
// Sender cố từ chối -> 403
mockMvc.perform(put("/api/requests/{requestId}/reject", sampleRequest.getRequestId()) ...)
      .andExpect(status().isForbidden());
```

```698:713:src/test/java/com/group02/openevent/service/impl/RequestServiceImplTest.java
// Overload approve(String): vẫn cập nhật Event PUBLIC
verify(eventService, times(1)).updateEventStatus(10L, EventStatus.PUBLIC);
```

```466:480:src/test/java/com/group02/openevent/service/EventServiceTest.java
// approveEvent -> set PUBLIC (logic nền tảng)
Event result = eventService.approveEvent(1L);
assertEquals(EventStatus.PUBLIC, result.getStatus());
```















