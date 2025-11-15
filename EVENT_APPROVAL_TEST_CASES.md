# Test Cases cho Event Approval Feature

## 📋 Tổng quan

Feature: **Event Approval Feature** (Nguyễn Trần Thành Duy)
- **Mô tả**: Feature cho phép Host tạo request phê duyệt event và Department có thể approve/reject request đó.
- **Status**: ✅ Đã có đầy đủ test cases

## 📊 Thống kê Test Cases

| Loại Test | Số lượng | File Test |
|-----------|----------|-----------|
| Unit Tests | 34 | `RequestServiceImplTest.java` |
| Integration Tests | 20 | `RequestControllerIntegrationTest.java` |
| **TỔNG** | **54** | |

## 🔍 Chi tiết Test Cases

### 1. Unit Tests (RequestServiceImplTest.java)

#### 1.1. Feature: createRequestWithFile (5 test cases)
- **UNIT-01**: Tạo request với file upload thành công ✅
- **UNIT-02**: Tạo request với file là null ✅
- **UNIT-03**: Tạo request với file rỗng (isEmpty) ✅
- **UNIT-04**: Ném ra RuntimeException khi upload file thất bại ✅
- **UNIT-05**: Ném ra RuntimeException khi Sender không tìm thấy ✅

#### 1.2. Feature: approveRequest (DTO) (3 test cases)
- **UNIT-06**: Approve request EVENT_APPROVAL thành công ✅
- **UNIT-07**: Ném ra RuntimeException khi request đã được xử lý ✅
- **UNIT-08**: Approve request (ví dụ: REFUND) thành công (không update event) ✅

#### 1.3. Feature: listRequests (Pagination Logic) (4 test cases)
- **UNIT-09**: Gọi đúng repo method (Status & Type) ✅
- **UNIT-10**: Gọi đúng repo method (Status only) ✅
- **UNIT-11**: Gọi đúng repo method (Type only) ✅
- **UNIT-12**: Gọi đúng repo method (Default - nulls) ✅

#### 1.4. Feature: convertToDTO (Mapper Logic) (1 test case)
- **UNIT-13**: Xử lý an toàn các quan hệ (relation) bị null ✅

#### 1.5. Feature: rejectRequest (DTO) (3 test cases)
- **Reject-01**: Từ chối request thành công ✅
- **Reject-02**: Ném lỗi khi từ chối request đã xử lý ✅
- **Reject-03**: Ném lỗi khi không tìm thấy request ✅

#### 1.6. Feature: createRequest (No File) (3 test cases)
- **Create-01**: Tạo request (không file) thành công ✅
- **Create-02**: Tạo request (không file, không event) thành công ✅
- **Create-03**: Ném lỗi khi event không tìm thấy ✅

#### 1.7. Feature: getRequestFormData (2 test cases)
- **FormData-01**: Lấy data cho form thành công ✅
- **FormData-02**: Ném lỗi khi event không tìm thấy ✅

#### 1.8. Feature: Simple Getters (List-based) (6 test cases)
- **Getter-01**: getAllRequests - Trả về danh sách DTO ✅
- **Getter-02**: getRequestsByStatus - Trả về danh sách DTO ✅
- **Getter-03**: getRequestsByType - Trả về danh sách DTO ✅
- **Getter-04**: getRequestsBySenderId - Trả về danh sách DTO ✅
- **Getter-05**: getRequestsByReceiverId - Trả về danh sách DTO ✅
- **Getter-06**: getRequestsByEventId - Trả về danh sách DTO ✅

#### 1.9. Feature: getRequestById (Optional-based) (2 test cases)
- **GetById-01**: Tìm thấy request ✅
- **GetById-02**: Không tìm thấy request ✅

#### 1.10. Feature: getRequestsByReceiver (Pageable) (2 test cases)
- **Pageable-01**: Gọi đúng repo method (Status != null) ✅
- **Pageable-02**: Gọi đúng repo method (Status == null) ✅

#### 1.11. Feature: listRequestsByReceiver (Pageable, DTO) (1 test case)
- **ListPageable-01**: Trả về DTO page ✅

#### 1.12. Feature: Overloaded Methods (String message) (2 test cases)
- **Overload-01**: approveRequest(String) - Hoạt động chính xác ✅
- **Overload-02**: rejectRequest(String) - Hoạt động chính xác ✅

### 2. Integration Tests (RequestControllerIntegrationTest.java)

#### 2.1. Feature: Request API Flow (Approve/CreateFile) (5 test cases)
- **INT-01**: Host tạo request và Department approve thành công (Happy Path - Full Flow) ✅
- **INT-02**: Host B không thể tạo request cho Event của Host A (AOP @RequireEventHost) ✅
- **INT-03**: Sender (Host A) không thể approve request của mình (AOP @RequireRequestReceiver) ✅
- **INT-04**: Dept D2 không thể approve request của Dept D1 (AOP @RequireRequestReceiver - Chéo) ✅
- **INT-05**: Approve request đã được approve ném ra 400 Bad Request (Edge Case - State) ✅

#### 2.2. Feature: Coverage cho PUT (Reject) và POST (Json) (6 test cases)
- **COVERAGE-Reject-01**: Receiver từ chối request thành công (Happy Path) ✅
- **COVERAGE-Reject-02**: Sender không thể từ chối request (AOP) ✅
- **COVERAGE-Reject-03**: Từ chối request đã xử lý ném ra 400 Bad Request (Logic) ✅
- **COVERAGE-CreateJson-01**: Ném lỗi 400 khi Event ID là null (DB constraint) ✅
- **COVERAGE-CreateJson-02**: Tạo request bằng JSON (với EventID) thành công (Happy Path) ✅
- **COVERAGE-CreateJson-03**: Ném lỗi khi logic service thất bại (Sender không tồn tại) ✅

#### 2.3. Feature: Coverage cho GET Endpoints (9 test cases)
- **COVERAGE-Get-01**: Lấy request bằng ID thành công (Happy Path) ✅
- **COVERAGE-Get-02**: Trả về 404 khi không tìm thấy (Not Found) ✅
- **COVERAGE-Get-03**: Lấy tất cả request (Branch 1: No Params) ✅
- **COVERAGE-Get-04**: Lấy request theo Status (Branch 2: Status Only) ✅
- **COVERAGE-Get-05**: Lấy request theo Type (Branch 3: Type Only) ✅
- **COVERAGE-Get-06**: Lấy request phân trang (getRequestsPaginated) ✅
- **COVERAGE-Get-07**: Lấy request theo Sender (getRequestsBySender) ✅
- **COVERAGE-Get-08**: Lấy request theo Receiver (getRequestsByReceiver) ✅
- **COVERAGE-Get-09**: Lấy request theo Event (getRequestsByEvent) ✅
- **COVERAGE-Get-10**: Lấy data cho form (showRequestForm) ✅

## 🎯 Test Coverage

### Các Scenarios Đã Cover

#### ✅ Create Request
- [x] Tạo request với file upload
- [x] Tạo request không file
- [x] Tạo request bằng JSON
- [x] Tạo request với event
- [x] Tạo request không event
- [x] Upload file thành công
- [x] Upload file thất bại
- [x] File null/empty
- [x] Sender/Receiver/Event không tồn tại

#### ✅ Approve Request
- [x] Approve request EVENT_APPROVAL thành công
- [x] Approve request khác type (không update event)
- [x] Approve request đã được xử lý (throw exception)
- [x] Approve request không tồn tại (throw exception)
- [x] Update event status sang PUBLIC khi approve EVENT_APPROVAL
- [x] AOP security: Chỉ receiver mới có thể approve

#### ✅ Reject Request
- [x] Reject request thành công
- [x] Reject request đã được xử lý (throw exception)
- [x] Reject request không tồn tại (throw exception)
- [x] AOP security: Chỉ receiver mới có thể reject
- [x] Không update event status khi reject

#### ✅ Get Requests
- [x] Get request by ID
- [x] Get all requests
- [x] Get requests by status
- [x] Get requests by type
- [x] Get requests by sender
- [x] Get requests by receiver
- [x] Get requests by event
- [x] Get requests paginated
- [x] Get request form data

#### ✅ Security & AOP
- [x] Host chỉ có thể tạo request cho event của mình (@RequireEventHost)
- [x] Chỉ receiver mới có thể approve/reject request (@RequireRequestReceiver)
- [x] Sender không thể approve/reject request của mình
- [x] Wrong receiver không thể approve/reject request

#### ✅ Error Handling
- [x] Request not found
- [x] Event not found
- [x] Sender not found
- [x] Receiver not found
- [x] Request already processed
- [x] Upload file failed
- [x] Database constraints violations

## 🚀 Chạy Tests

```bash
# Chạy tất cả unit tests
mvn test -Dtest=RequestServiceImplTest

# Chạy tất cả integration tests
mvn test -Dtest=RequestControllerIntegrationTest

# Chạy cả hai
mvn test -Dtest=RequestServiceImplTest,RequestControllerIntegrationTest

# Chạy với coverage report
mvn test jacoco:report -Dtest=RequestServiceImplTest,RequestControllerIntegrationTest
```

## 📁 Files Test

1. **Unit Tests**: `src/test/java/com/group02/openevent/service/impl/RequestServiceImplTest.java`
2. **Integration Tests**: `src/test/java/com/group02/openevent/controller/RequestControllerIntegrationTest.java`

## ✅ Kết luận

Event Approval Feature đã có **54 test cases** đầy đủ bao gồm:
- ✅ Unit tests cho tất cả service methods
- ✅ Integration tests cho tất cả controller endpoints
- ✅ AOP security aspects testing
- ✅ Error handling và edge cases
- ✅ Happy paths và negative scenarios
- ✅ Database operations và transactions

**Coverage**: High - Tất cả các scenarios chính đều đã được cover.



