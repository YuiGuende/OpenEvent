# Event Approval – Decision Table Coverage

Nguồn đối chiếu: docs/testing/Decision_Table_Event_Approval.docx  
Scope mã và test: `src/main/java/**/*request*/**`, `src/main/java/**/*event*/**`, `src/test/java/**/*request*/**`, `src/test/java/**/*event*/**`

Legend:
- Covered?: Yes / Partial / Missing
- Evidence: class#method + key asserts/verifies (kèm code reference)

## Bảng 1 – Approve Request (EVENT_APPROVAL): A1–A5

| Table | Rule | Covered? | Evidence (class#method, key asserts/verifies) | Gap / Fix |
|------|------|----------|-----------------------------------------------|-----------|
| Approve | A1 | Yes | RequestControllerIntegrationTest#whenHostCreatesRequestAndReceiverApproves_thenFlowSucceeds → 200; Event=PUBLIC |  |
| Approve | A2 | Yes | RequestControllerIntegrationTest#whenApprovingAlreadyApprovedRequest_thenServiceThrowsException → 400 |  |
| Approve | A3 | Yes | RequestControllerIntegrationTest#whenWrongReceiverTriesToApproveRequest_thenAopDeniesAccess → 403 |  |
| Approve | A4 | Yes | RequestServiceImplTest#whenApproveOtherRequestType_thenSetStatusApprovedAndNoEventUpdate → APPROVED; no Event update | Service-level; đủ theo rule |
| Approve | A5 | Missing | – | Cần test approve khi requestId không tồn tại ⇒ 404/400 |

Code references:

```188:225:src/test/java/com/group02/openevent/controller/RequestControllerIntegrationTest.java
@DisplayName("INT-01 ... Department approve") ... status().isOk(); ... assertThat(updatedEvent.getStatus()).isEqualTo(EventStatus.PUBLIC);
```

```276:293:src/test/java/com/group02/openevent/controller/RequestControllerIntegrationTest.java
@DisplayName("INT-05 ... đã được approve ném ra 400") ... status().isBadRequest();
```

```262:274:src/test/java/com/group02/openevent/controller/RequestControllerIntegrationTest.java
@DisplayName("INT-04 ... Dept D2 không thể approve") ... status().isForbidden();
```

```263:286:src/test/java/com/group02/openevent/service/impl/RequestServiceImplTest.java
@DisplayName("UNIT-08 ... loại khác") ... status APPROVED; verify(eventService, never()).updateEventStatus(...);
```

## Bảng 2 – Reject Request: R1–R4

| Table | Rule | Covered? | Evidence (class#method, key asserts/verifies) | Gap / Fix |
|------|------|----------|-----------------------------------------------|-----------|
| Reject | R1 | Yes | RequestControllerIntegrationTest#whenReceiverRejectsRequest_thenSucceeds → 200; status=REJECTED |  |
| Reject | R2 | Yes | RequestControllerIntegrationTest#whenRejectingAlreadyProcessedRequest_thenBadRequest → 400 |  |
| Reject | R3 | Yes | RequestControllerIntegrationTest#whenSenderRejectsRequest_thenForbidden → 403 |  |
| Reject | R4 | Partial | RequestServiceImplTest#whenRejectNotFound_thenThrowException → "Request not found" (service) | Đề xuất bổ sung integration test 404/400 |

References:

```304:321:src/test/java/com/group02/openevent/controller/RequestControllerIntegrationTest.java
Reject happy → status().isOk(); jsonPath("$.status").value("REJECTED");
```

```339:355:src/test/java/com/group02/openevent/controller/RequestControllerIntegrationTest.java
Reject processed → status().isBadRequest();
```

```414:424:src/test/java/com/group02/openevent/service/impl/RequestServiceImplTest.java
Reject not found → RuntimeException("Request not found")
```

## Bảng 3 – Create Approval Request (Host → Department): C1–C5

| Table | Rule | Covered? | Evidence (class#method, key asserts/verifies) | Gap / Fix |
|------|------|----------|-----------------------------------------------|-----------|
| Create | C1 | Yes | RequestControllerIntegrationTest#INT-01 (multipart) → 201; status=PENDING |  |
| Create | C2 | Yes | RequestControllerIntegrationTest#whenCreateRequestJsonWithEvent_thenSucceeds → 201 |  |
| Create | C3 | Yes | RequestControllerIntegrationTest#INT-02 (wrong host) → 403 |  |
| Create | C4 | Yes | RequestServiceImplTest#whenCreateRequestEventNotFound_thenThrowException (service); RequestControllerIntegrationTest#whenCreateRequestJsonWithNullEvent_thenBadRequest / ...Fails_thenBadRequest |  |
| Create | C5 | Yes | RequestServiceImplTest#whenCreateRequestWithNullFile_thenUploadSkippedAndRequestSaved / ...EmptyFile... → fileURL null | Service-level; đủ theo rule |

References:

```193:205:src/test/java/com/group02/openevent/controller/RequestControllerIntegrationTest.java
Create multipart → status().isCreated(); jsonPath("$.status").value("PENDING");
```

```381:399:src/test/java/com/group02/openevent/controller/RequestControllerIntegrationTest.java
Create JSON happy → status().isCreated();
```

```228:243:src/test/java/com/group02/openevent/controller/RequestControllerIntegrationTest.java
Wrong host → status().isForbidden();
```

```474:486:src/test/java/com/group02/openevent/service/impl/RequestServiceImplTest.java
Event not found → RuntimeException("Event not found");
```

## Bảng 4 – Filter/List Request: G1–G4

| Table | Rule | Covered? | Evidence (class#method, key asserts/verifies) | Gap / Fix |
|------|------|----------|-----------------------------------------------|-----------|
| List | G1 | Yes | RequestControllerIntegrationTest#whenGetAllRequestsNoParams_thenSucceeds → 200 + array contains id |  |
| List | G2 | Yes | RequestControllerIntegrationTest#whenGetAllRequestsByStatus_thenSucceeds → 200 + status=PENDING |  |
| List | G3 | Yes | RequestControllerIntegrationTest#whenGetAllRequestsByType_thenSucceeds → 200 + type=EVENT_APPROVAL |  |
| List | G4 | Yes | RequestServiceImplTest#whenListWithStatusAndType_thenCallFindByStatusAndType (repo verify) | Service-level; đủ theo rule |

References:

```452:461:src/test/java/com/group02/openevent/controller/RequestControllerIntegrationTest.java
No filters → status().isOk() ... hasItem(requestId)
```

```464:473:src/test/java/com/group02/openevent/controller/RequestControllerIntegrationTest.java
Status only → status().isOk(); jsonPath("$[0].status").value("PENDING");
```

```476:485:src/test/java/com/group02/openevent/controller/RequestControllerIntegrationTest.java
Type only → status().isOk(); jsonPath("$[0].type").value("EVENT_APPROVAL");
```

```297:305:src/test/java/com/group02/openevent/service/impl/RequestServiceImplTest.java
Repo verify: findByStatusAndType(...)
```

---

## Summary
- Total rules: 4 tables × (5 + 4 + 5 + 4) = 18
- Covered: 16
- Partial: 1 (R4)
- Missing: 1 (A5)
- Coverage: 16/18 ≈ 88.9%

⚠️ Critical thiếu:
- A5: Approve khi request không tồn tại ⇒ 404/400
- R4: Reject khi request không tồn tại ⇒ hiện có service test, thiếu integration test HTTP (404/400)

---

## JUnit5 + Mockito + AssertJ Skeletons cho rule thiếu

Lưu ý: Chỉnh lại package/controller path theo repo của bạn. Ví dụ dưới dùng `RequestController` với endpoint:
- PUT `/api/requests/{requestId}/approve`
- PUT `/api/requests/{requestId}/reject`

```java
package com.group02.openevent.controller.request;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.group02.openevent.dto.requestApproveEvent.ApproveRequestDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = RequestController.class)
class RequestControllerMissingCasesTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private com.group02.openevent.service.impl.RequestServiceImpl requestService; // hoặc RequestService

    @Test
    @DisplayName("A5: should return 404/400 when approving non-existent request")
    void shouldReturnNotFound_whenApproveRequestNotExists_A5() throws Exception {
        // Given
        ApproveRequestDTO dto = new ApproveRequestDTO("Approved");
        // Tuỳ GlobalExceptionHandler ánh xạ, có thể ném RuntimeException hoặc EntityNotFoundException
        doThrow(new RuntimeException("Request not found")).when(requestService).approveRequest(anyLong(), org.mockito.ArgumentMatchers.any(ApproveRequestDTO.class));

        // When + Then
        mockMvc.perform(put("/api/requests/{requestId}/approve", 99999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                // Chọn 1 trong 2 tuỳ mapping của handler
                .andExpect(status().isNotFound()); // hoặc .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("R4: should return 404/400 when rejecting non-existent request")
    void shouldReturnNotFound_whenRejectRequestNotExists_R4() throws Exception {
        // Given
        ApproveRequestDTO dto = new ApproveRequestDTO("Rejected");
        doThrow(new RuntimeException("Request not found")).when(requestService).rejectRequest(anyLong(), org.mockito.ArgumentMatchers.any(ApproveRequestDTO.class));

        // When + Then
        mockMvc.perform(put("/api/requests/{requestId}/reject", 99999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isNotFound()); // hoặc .andExpect(status().isBadRequest());
    }
}
```

Nếu muốn bám sát style Integration hiện hữu (`@SpringBootTest` + `@AutoConfigureMockMvc` + data setup), có thể nhân bản cấu trúc của `RequestControllerIntegrationTest` và thêm 2 test trên, cập nhật khoá lỗi theo `GlobalExceptionHandler` để kỳ vọng 404 hay 400 cho đúng thực tế dự án.















