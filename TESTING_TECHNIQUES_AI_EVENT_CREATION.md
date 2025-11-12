# Testing Techniques cho Feature: Tạo Event bằng AI

## 📋 Tổng quan

Feature **Tạo Event bằng AI** cho phép người dùng tạo sự kiện từ câu lệnh tự nhiên thông qua AI. Feature này bao gồm:
- Parse thông tin từ câu lệnh tự nhiên
- Validation và security checks
- Kiểm tra xung đột thời gian/địa điểm
- Cảnh báo thời tiết cho sự kiện ngoài trời
- Tích hợp với LLM và vector database

---

## 🎯 Testing Strategy Overview

### Testing Pyramid cho AI Event Creation

```
                    ┌─────────────┐
                    │   E2E Tests │  (10%)
                    │  Integration│
                    └─────────────┘
                  ┌───────────────────┐
                  │ Integration Tests │  (30%)
                  │  (API, Services)  │
                  └───────────────────┘
              ┌───────────────────────────┐
              │      Unit Tests           │  (60%)
              │ (Services, Mappers, Utils)│
              └───────────────────────────┘
```

---

## 1. Unit Testing Techniques

### 1.1. Boundary Value Analysis (BVA)

**Áp dụng cho:** Validation logic, DateTime parsing, User ID validation

**Ví dụ Test Cases:**

```java
@Test
void testUserIdValidation_BoundaryValues() {
    // Lower boundary: 0 (invalid)
    assertThrows(IllegalArgumentException.class, 
        () -> controller.createEvent(createRequestWithUserId(0)));
    
    // Lower boundary: 1 (valid)
    assertDoesNotThrow(() -> 
        controller.createEvent(createRequestWithUserId(1)));
    
    // Upper boundary: Long.MAX_VALUE (valid)
    assertDoesNotThrow(() -> 
        controller.createEvent(createRequestWithUserId(Long.MAX_VALUE)));
    
    // Negative boundary: -1 (invalid)
    assertThrows(IllegalArgumentException.class, 
        () -> controller.createEvent(createRequestWithUserId(-1)));
}
```

**Test Cases:**
- ✅ User ID = 0 (invalid boundary)
- ✅ User ID = 1 (valid boundary)
- ✅ User ID = -1 (invalid boundary)
- ✅ User ID = Long.MAX_VALUE (valid upper boundary)
- ✅ User ID = null (invalid)

### 1.2. Equivalence Partitioning (EP)

**Áp dụng cho:** DateTime parsing, Event type classification, Tool name validation

**Ví dụ Test Cases:**

```java
@Test
void testDateTimeParsing_EquivalenceClasses() {
    // Valid formats (Equivalence Class 1)
    String[] validFormats = {
        "2024-12-25T10:00",      // ISO format
        "2024-12-25 10:00",      // Space format
        "25/12/2024 10:00",      // DD/MM/YYYY
        "25-12-2024 10:00"       // DD-MM-YYYY
    };
    
    for (String dateStr : validFormats) {
        assertDoesNotThrow(() -> 
            parseDateTime(dateStr));
    }
    
    // Invalid formats (Equivalence Class 2)
    String[] invalidFormats = {
        "2024/12/25",            // Wrong separator
        "25 Dec 2024",           // Text month
        "invalid",                // Completely invalid
        ""                        // Empty
    };
    
    for (String dateStr : invalidFormats) {
        assertThrows(IllegalArgumentException.class, 
            () -> parseDateTime(dateStr));
    }
}
```

**Equivalence Classes:**
- **Valid DateTime Formats:** ISO, Space, DD/MM/YYYY, DD-MM-YYYY
- **Invalid DateTime Formats:** Wrong separator, Text format, Empty, Null

### 1.3. Decision Table Testing

**Áp dụng cho:** Business rules validation, Weather warning logic

**Ví dụ Decision Table:**

| C1: User Valid | C2: Action Valid | C3: Tool Name | C4: Fields Complete | C5: Time Valid | Expected Result |
|----------------|------------------|---------------|---------------------|----------------|------------------|
| T | T | ADD_EVENT | T | T | ✅ Create Event |
| F | T | ADD_EVENT | T | T | ❌ 400: User ID invalid |
| T | F | ADD_EVENT | T | T | ❌ 400: Action null |
| T | T | UPDATE_EVENT | T | T | ❌ 400: Invalid tool |
| T | T | ADD_EVENT | F | T | ❌ 400: Missing fields |
| T | T | ADD_EVENT | T | F | ❌ 400: Time invalid |

**Implementation:**

```java
@ParameterizedTest
@CsvSource({
    "true, true, ADD_EVENT, true, true, SUCCESS",
    "false, true, ADD_EVENT, true, true, USER_INVALID",
    "true, false, ADD_EVENT, true, true, ACTION_NULL",
    "true, true, UPDATE_EVENT, true, true, INVALID_TOOL",
    "true, true, ADD_EVENT, false, true, MISSING_FIELDS",
    "true, true, ADD_EVENT, true, false, TIME_INVALID"
})
void testEventCreation_DecisionTable(
    boolean userValid, boolean actionValid, 
    String toolName, boolean fieldsComplete, 
    boolean timeValid, String expectedResult) {
    
    // Setup test data based on parameters
    // Assert expected result
}
```

### 1.4. State Transition Testing

**Áp dụng cho:** Event creation flow với pending events, weather warnings

**States:**
1. **INITIAL** → User sends request
2. **PARSING** → Parsing datetime and extracting info
3. **VALIDATING** → Validating input
4. **CHECKING_CONFLICT** → Checking time conflicts
5. **CHECKING_WEATHER** → Checking weather (if outdoor)
6. **PENDING_CONFIRMATION** → Waiting for user confirmation (if warning)
7. **CREATING** → Creating event
8. **SUCCESS** → Event created
9. **ERROR** → Error occurred

**Test Cases:**
- ✅ Normal flow: INITIAL → PARSING → VALIDATING → CREATING → SUCCESS
- ✅ With conflict: INITIAL → ... → CHECKING_CONFLICT → ERROR
- ✅ With weather warning: INITIAL → ... → CHECKING_WEATHER → PENDING_CONFIRMATION → CREATING → SUCCESS
- ✅ User rejects: INITIAL → ... → PENDING_CONFIRMATION → ERROR

### 1.5. Error Guessing

**Áp dụng cho:** Edge cases, unexpected inputs

**Test Cases:**
- ✅ SQL Injection trong title: `"'; DROP TABLE events; --"`
- ✅ XSS trong description: `"<script>alert('xss')</script>"`
- ✅ Very long title (>200 characters)
- ✅ Special characters trong datetime: `"2024-12-25T10:00:00.000Z"`
- ✅ Timezone issues: `"2024-12-25T10:00+07:00"`
- ✅ Concurrent requests từ cùng user
- ✅ Memory leak với large batch requests

---

## 2. Integration Testing Techniques

### 2.1. API Integration Tests

**Áp dụng cho:** `EventAIController` endpoints

**Test Structure:**

```java
@WebMvcTest(EventAIController.class)
class EventAIControllerIntegrationTest {
    
    @Test
    void testCreateEvent_Success() throws Exception {
        // Given
        Map<String, Object> request = Map.of(
            "userId", 1,
            "action", Map.of(
                "toolName", "ADD_EVENT",
                "args", Map.of(
                    "title", "Test Event",
                    "start_time", "2024-12-25T10:00",
                    "end_time", "2024-12-25T12:00"
                )
            )
        );
        
        // When
        mockMvc.perform(post("/api/ai/event/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        
        // Then
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.message").value("✅ Đã tạo sự kiện thành công"));
    }
}
```

**Test Scenarios:**
- ✅ Happy path: Valid request → Success
- ✅ Invalid userId → 400 Bad Request
- ✅ Invalid action → 400 Bad Request
- ✅ Missing required fields → 400 Bad Request
- ✅ Time conflict → 409 Conflict (or warning)
- ✅ Weather warning → 200 with warning flag

### 2.2. Service Integration Tests

**Áp dụng cho:** `AgentEventService`, `EventAIAgent`

**Test Cases:**

```java
@SpringBootTest
@Transactional
class AgentEventServiceIntegrationTest {
    
    @Test
    void testSaveEventFromAction_WithValidData() {
        // Given
        Action action = createValidAction();
        Long userId = 1L;
        
        // When
        agentEventService.saveEventFromAction(action, userId);
        
        // Then
        List<Event> events = eventService.getEventByHostId(userId);
        assertThat(events).hasSize(1);
        assertThat(events.get(0).getTitle())
            .isEqualTo(action.getArgs().get("title"));
    }
    
    @Test
    void testSaveEventFromAction_WithTimeConflict() {
        // Given: Existing event at same time
        Event existing = createEvent("2024-12-25T10:00", "2024-12-25T12:00");
        eventService.saveEvent(existing);
        
        Action action = createAction("2024-12-25T10:30", "2024-12-25T11:30");
        
        // When/Then
        assertThrows(TimeConflictException.class, 
            () -> agentEventService.saveEventFromAction(action, 1L));
    }
}
```

### 2.3. Database Integration Tests

**Áp dụng cho:** Event persistence, Host creation, Organization linking

**Test Cases:**
- ✅ Event được lưu vào database với đúng thông tin
- ✅ Host tự động được tạo nếu Customer chưa có Host
- ✅ Organization được link đúng nếu có organizationId
- ✅ Email reminder được tạo mặc định
- ✅ Transaction rollback khi có lỗi

### 2.4. External Service Integration Tests

**Áp dụng cho:** Weather Service, LLM Service, Qdrant Service

**Test Cases với Mocking:**

```java
@Test
void testWeatherWarning_OutdoorEvent() {
    // Given
    when(weatherService.getForecast(any(), any()))
        .thenReturn(createRainyForecast());
    
    when(classifier.classifyWeather(anyString()))
        .thenReturn("outdoor_activities");
    
    Action action = createOutdoorEventAction();
    
    // When
    ResponseEntity<Map<String, Object>> response = 
        controller.createEvent(createRequest(action, 1));
    
    // Then
    assertThat(response.getBody().get("hasWeatherWarning"))
        .isEqualTo(true);
}
```

**Test Scenarios:**
- ✅ Weather service returns valid forecast
- ✅ Weather service fails → Graceful degradation
- ✅ LLM service timeout → Retry logic
- ✅ Qdrant service unavailable → Fallback to DB search

---

## 3. System Testing Techniques

### 3.1. End-to-End (E2E) Testing

**Áp dụng cho:** Complete user flow từ chat input đến event creation

**Test Flow:**

```java
@Test
void testE2E_CreateEventFromNaturalLanguage() {
    // 1. User sends natural language request
    String userInput = "Tạo sự kiện workshop Spring Boot vào 25/12/2024 lúc 10:00";
    
    // 2. AI processes and extracts intent
    String response = eventAIAgent.processMessage(userInput, sessionId, userId);
    
    // 3. System creates pending event
    assertThat(pendingEvents).containsKey(sessionId);
    
    // 4. User confirms
    String confirmResponse = eventAIAgent.processMessage("Có", sessionId, userId);
    
    // 5. Event is created
    assertThat(confirmResponse).contains("Đã tạo sự kiện");
    
    // 6. Verify event in database
    List<Event> events = eventService.getEventByHostId(userId);
    assertThat(events).hasSize(1);
    assertThat(events.get(0).getTitle()).contains("Spring Boot");
}
```

### 3.2. Performance Testing

**Test Cases:**
- ✅ Response time < 2s cho event creation
- ✅ Concurrent requests (100 users) → No deadlock
- ✅ Large batch processing (1000 events) → Memory efficient
- ✅ Database query optimization → < 100ms per query

**Tools:** JMeter, Gatling, K6

### 3.3. Security Testing

**Test Cases:**
- ✅ SQL Injection: `"title": "'; DROP TABLE events; --"`
- ✅ XSS: `"description": "<script>alert('xss')</script>"`
- ✅ Command Injection: `"title": "test; rm -rf /"`
- ✅ Path Traversal: `"title": "../../etc/passwd"`
- ✅ Rate Limiting: 100 requests/second → Blocked
- ✅ Authentication: Unauthorized access → 401

**Implementation:**

```java
@Test
void testSecurity_SQLInjection() {
    Action action = createAction();
    action.getArgs().put("title", "'; DROP TABLE events; --");
    
    ResponseEntity<Map<String, Object>> response = 
        controller.createEvent(createRequest(action, 1));
    
    // Should be sanitized or rejected
    assertThat(response.getStatusCode())
        .isEqualTo(HttpStatus.BAD_REQUEST);
}
```

### 3.4. Usability Testing

**Test Cases:**
- ✅ Natural language variations được hiểu đúng
- ✅ Error messages rõ ràng, dễ hiểu
- ✅ Confirmation flow intuitive
- ✅ Weather warnings helpful

---

## 4. Specialized Testing Techniques

### 4.1. AI/ML Model Testing

**Áp dụng cho:** Intent classification, Entity extraction, Language detection

**Test Cases:**
- ✅ Intent classification accuracy > 90%
- ✅ Entity extraction precision/recall
- ✅ Language detection cho Vietnamese/English
- ✅ Fallback khi model fails

### 4.2. Data-Driven Testing

**Áp dụng cho:** Multiple input variations

```java
@ParameterizedTest
@CsvFileSource(resources = "/test-data/event-creation-inputs.csv")
void testEventCreation_DataDriven(
    String input, String expectedTitle, 
    String expectedStart, String expectedEnd) {
    
    // Test với nhiều variations của input
}
```

**CSV Format:**
```csv
input,expectedTitle,expectedStart,expectedEnd
"Tạo event vào 25/12 lúc 10h","event","2024-12-25T10:00","2024-12-25T11:00"
"Workshop Spring Boot ngày mai","Workshop Spring Boot","2024-12-26T09:00","2024-12-26T17:00"
```

### 4.3. Mutation Testing

**Áp dụng cho:** Test quality assessment

**Tools:** PIT (Pitest), Major

**Mục tiêu:** Mutation score > 80%

### 4.4. Property-Based Testing

**Áp dụng cho:** DateTime parsing, Validation logic

**Tools:** JUnit-Quickcheck, jqwik

```java
@Property
void testDateTimeParsing_PropertyBased(
    @ForAll @InRange(min = "2020-01-01T00:00", max = "2030-12-31T23:59") 
    LocalDateTime dateTime) {
    
    String formatted = formatDateTime(dateTime);
    LocalDateTime parsed = parseDateTime(formatted);
    
    assertThat(parsed).isEqualTo(dateTime);
}
```

---

## 5. Test Coverage Goals

### Unit Tests
- **Target:** 80%+ line coverage
- **Critical paths:** 100% coverage
  - Validation logic
  - DateTime parsing
  - Security checks
  - Error handling

### Integration Tests
- **Target:** All API endpoints
- **Target:** All service methods
- **Target:** All database operations

### E2E Tests
- **Target:** Critical user flows
  - Happy path
  - Error scenarios
  - Weather warning flow
  - Conflict resolution

---

## 6. Test Data Management

### 6.1. Test Fixtures

```java
public class EventTestFixtures {
    public static Action createValidAction() {
        Action action = new Action();
        action.setToolName("ADD_EVENT");
        action.setArgs(Map.of(
            "title", "Test Event",
            "start_time", "2024-12-25T10:00",
            "end_time", "2024-12-25T12:00",
            "description", "Test description"
        ));
        return action;
    }
    
    public static Event createEvent(String start, String end) {
        Event event = new Event();
        event.setTitle("Existing Event");
        event.setStartsAt(LocalDateTime.parse(start));
        event.setEndsAt(LocalDateTime.parse(end));
        return event;
    }
}
```

### 6.2. Test Database

- ✅ Use H2 in-memory database cho unit tests
- ✅ Use Testcontainers cho integration tests
- ✅ Clean database state sau mỗi test

---

## 7. Test Automation Strategy

### 7.1. CI/CD Pipeline

```
┌─────────────┐
│   Commit    │
└──────┬──────┘
       │
       ▼
┌─────────────────┐
│  Unit Tests     │  (Fast, < 1 min)
└──────┬──────────┘
       │
       ▼
┌─────────────────┐
│ Integration     │  (Medium, < 5 min)
│ Tests           │
└──────┬──────────┘
       │
       ▼
┌─────────────────┐
│ E2E Tests       │  (Slow, < 15 min)
└──────┬──────────┘
       │
       ▼
┌─────────────────┐
│   Deploy        │
└─────────────────┘
```

### 7.2. Test Execution

- **Pre-commit:** Unit tests only
- **Pull Request:** Unit + Integration tests
- **Main branch:** All tests including E2E
- **Nightly:** Full test suite + Performance tests

---

## 8. Recommended Testing Tools

### Unit Testing
- ✅ **JUnit 5** - Test framework
- ✅ **Mockito** - Mocking framework
- ✅ **AssertJ** - Fluent assertions
- ✅ **jqwik** - Property-based testing

### Integration Testing
- ✅ **Spring Boot Test** - @SpringBootTest
- ✅ **Testcontainers** - Database containers
- ✅ **WireMock** - HTTP service mocking
- ✅ **MockMvc** - Web layer testing

### E2E Testing
- ✅ **Selenium** - Browser automation
- ✅ **Playwright** - Modern E2E testing
- ✅ **REST Assured** - API E2E testing

### Performance Testing
- ✅ **JMeter** - Load testing
- ✅ **Gatling** - Performance testing
- ✅ **K6** - Modern load testing

### Code Quality
- ✅ **JaCoCo** - Code coverage
- ✅ **PIT** - Mutation testing
- ✅ **SonarQube** - Code quality analysis

---

## 9. Test Case Examples

### Example 1: Unit Test - DateTime Parsing

```java
@ParameterizedTest
@CsvSource({
    "2024-12-25T10:00, 2024-12-25T10:00",
    "2024-12-25 10:00, 2024-12-25T10:00",
    "25/12/2024 10:00, 2024-12-25T10:00",
    "25-12-2024 10:00, 2024-12-25T10:00"
})
void testParseDateTime_ValidFormats(String input, String expected) {
    LocalDateTime result = tryParseDateTime(input);
    assertThat(result).isEqualTo(LocalDateTime.parse(expected));
}

@Test
void testParseDateTime_InvalidFormat() {
    assertThrows(IllegalArgumentException.class, 
        () -> tryParseDateTime("invalid-date"));
}
```

### Example 2: Integration Test - Event Creation

```java
@SpringBootTest
@AutoConfigureMockMvc
class EventAIControllerIntegrationTest {
    
    @Test
    void testCreateEvent_CompleteFlow() throws Exception {
        // Given
        Map<String, Object> request = createValidRequest();
        
        // When
        MvcResult result = mockMvc.perform(
            post("/api/ai/event/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
        .andExpect(status().isOk())
        .andReturn();
        
        // Then
        Map<String, Object> response = objectMapper.readValue(
            result.getResponse().getContentAsString(), 
            Map.class
        );
        
        assertThat(response.get("success")).isEqualTo(true);
        
        // Verify event in database
        List<Event> events = eventService.getEventByHostId(1L);
        assertThat(events).hasSize(1);
    }
}
```

### Example 3: E2E Test - Natural Language Processing

```java
@Test
void testE2E_NaturalLanguageToEvent() {
    // Given
    String userInput = "Tạo workshop Spring Boot vào ngày 25 tháng 12 lúc 10 giờ sáng";
    
    // When
    String response = eventAIAgent.processMessage(userInput, "session-1", 1L);
    
    // Then
    assertThat(response).contains("xác nhận");
    assertThat(pendingEvents).containsKey("session-1");
    
    // User confirms
    String confirmResponse = eventAIAgent.processMessage("Có", "session-1", 1L);
    
    // Verify event created
    assertThat(confirmResponse).contains("Đã tạo sự kiện");
    List<Event> events = eventService.getEventByHostId(1L);
    assertThat(events).anyMatch(e -> e.getTitle().contains("Spring Boot"));
}
```

---

## 10. Best Practices

### ✅ DO:
- Write tests before fixing bugs (TDD)
- Use descriptive test names
- Follow AAA pattern (Arrange-Act-Assert)
- Keep tests independent
- Use test fixtures for reusability
- Mock external dependencies
- Test edge cases and boundaries
- Maintain high test coverage

### ❌ DON'T:
- Test implementation details
- Write flaky tests
- Share test data between tests
- Skip error scenarios
- Write tests that depend on execution order
- Test third-party libraries
- Write slow tests in unit test suite

---

## 11. Metrics & Reporting

### Key Metrics:
- **Code Coverage:** > 80%
- **Test Execution Time:** < 10 minutes (full suite)
- **Test Pass Rate:** > 95%
- **Flaky Test Rate:** < 1%
- **Bug Detection Rate:** > 70% bugs caught by tests

### Reporting Tools:
- **JaCoCo** - Coverage reports
- **Allure** - Test reports
- **TestNG** - HTML reports
- **Jenkins/GitHub Actions** - CI/CD reports

---

## 📚 Tài liệu tham khảo

- BDD Test Cases: `BDD_TEST_CASES_EVENT_MANAGEMENT_AI.md`
- Decision Table: `DECISION_TABLE_EVENT_MANAGEMENT_AI.md`
- Source Code:
  - Controller: `EventAIController.java`
  - Service: `EventAIAgent.java`, `AgentEventService.java`
  - Mapper: `AIEventMapper.java`

---

## 📝 Kết luận

Feature **Tạo Event bằng AI** cần được test kỹ lưỡng với nhiều kỹ thuật khác nhau:
- **Unit Tests** cho logic nghiệp vụ
- **Integration Tests** cho API và services
- **E2E Tests** cho user flows
- **Security Tests** cho input validation
- **Performance Tests** cho scalability

Việc kết hợp các kỹ thuật này đảm bảo feature hoạt động đúng, an toàn và hiệu quả.












