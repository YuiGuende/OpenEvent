# 📊 SEQUENCE DIAGRAM - TẠO SỰ KIỆN QUA AI (BẢN CẢI THIỆN)

## ✅ Đánh giá Sequence Diagram hiện tại

### **Các điểm đúng:**
1. ✅ Luồng tổng quát đúng
2. ✅ Các bước chính được bao quát
3. ✅ Thứ tự các bước hợp lý

### **Các điểm cần bổ sung:**

1. ❌ **Thiếu EmbeddingService** - Cần để tạo vector cho intent classification và place search
2. ❌ **Thiếu PlaceService** - Cần để tìm place từ DB sau khi có place_id từ Qdrant
3. ❌ **Thiếu ChatMessageRepo** - Cần để load conversation context từ DB
4. ❌ **Thiếu các service trong AgentEventService**: CustomerService, HostService, OrganizationService, AIEventMapper
5. ❌ **Thiếu bước load conversation context** - buildConversationContext() query DB
6. ❌ **Thiếu bước kiểm tra special cases** - isOutOfScope(), isWeatherQuestion()
7. ❌ **Thiếu weather classification** - classifyWeather() trước khi gọi WeatherService
8. ❌ **Thiếu response translation** - Dịch response về ngôn ngữ gốc ở Controller
9. ❌ **Thiếu bước tạo embedding cho place** - Trước khi search Qdrant
10. ❌ **Thiếu bước tạo EventItem** - Giữa validation và gọi AgentEventService

---

## 🔄 SEQUENCE DIAGRAM ĐẦY ĐỦ (Mermaid)

```mermaid
sequenceDiagram
    autonumber
    
    actor User as User
    participant View as View (chatbot.js)
    participant Ctrl as EnhancedAIController
    participant RateLimit as RateLimitingService
    participant Security as AISecurityService
    participant Lang as LanguageDetectionService
    participant Trans as TranslationService
    participant ChatSvc as ChatSessionService
    participant ChatMsgRepo as ChatMessageRepo
    participant Agent as EventAIAgent
    participant EmbedSvc as EmbeddingService
    participant VIC as VectorIntentClassifier
    participant LLM as LLM Service
    participant PlaceSvc as PlaceService
    participant Qdrant as QdrantService
    participant EvSvc as EventService
    participant AgentEvt as AgentEventService
    participant CustSvc as CustomerService
    participant HostSvc as HostService
    participant OrgSvc as OrganizationService
    participant EventMapper as AIEventMapper
    participant CustRepo as CustomerRepository
    participant HostRepo as HostRepository
    participant OrgRepo as OrganizationRepository
    participant EventRepo as EventRepository
    participant EmailRepo as EmailReminderRepository
    participant Weather as WeatherService
    participant DB as Database
    
    User->>View: "Tạo sự kiện Music Night..."
    View->>Ctrl: POST /api/ai/chat/enhanced<br/>(message, sessionId)
    
    Note over Ctrl: Step 2: Validation & Security
    
    Ctrl->>Ctrl: Get userId from session
    Ctrl->>RateLimit: isAllowed(userId, AI_CHAT)
    RateLimit-->>Ctrl: allowed: true/false
    
    alt Rate limit exceeded
        Ctrl-->>View: 429 Rate Limit Exceeded
        View-->>User: Error message
    else Rate limit OK
        Ctrl->>Security: validateInput(message, MESSAGE)
        Security-->>Ctrl: ValidationResult
        
        alt Validation failed
            Ctrl-->>View: 400 Bad Request
            View-->>User: Error message
        else Validation OK
            Note over Ctrl: Step 3: Language Detection & Translation
            
            Ctrl->>Lang: detectLanguage(message)
            Lang-->>Ctrl: userLanguage (e.g., VIETNAMESE)
            
            alt userLanguage != VIETNAMESE
                Ctrl->>Trans: translateUserInput(message, userLanguage)
                Trans-->>Ctrl: translatedMessage (Vietnamese)
            else userLanguage == VIETNAMESE
                Note over Ctrl: Use original message
            end
            
            Ctrl->>ChatSvc: chat(ChatRequest)
            
            Note over ChatSvc: Step 4: Save User Message
            
            ChatSvc->>ChatMsgRepo: save(userMessage)
            ChatMsgRepo->>DB: INSERT INTO chat_message
            DB-->>ChatMsgRepo: Saved
            ChatMsgRepo-->>ChatSvc: User message saved
            
            ChatSvc->>Agent: reply(userInput, userId, sessionId)
            
            Note over Agent: Step 5.1: Build Context
            
            Agent->>Agent: buildSystemPrompt()
            Agent->>ChatMsgRepo: findByUserIdAndSessionIdOrderByTimestampAsc(userId, sessionId)
            ChatMsgRepo->>DB: SELECT * FROM chat_message<br/>WHERE session_id = ? ORDER BY timestamp
            DB-->>ChatMsgRepo: Last 10 messages
            ChatMsgRepo-->>Agent: Conversation history
            Agent->>Agent: buildConversationContext()<br/>(system + history)
            
            Note over Agent: Step 5.2: Check Special Cases
            
            Agent->>Agent: isOutOfScope(userInput)
            alt Out of scope
                Agent-->>ChatSvc: handleOutOfScopeQuestion()
                ChatSvc-->>Ctrl: Response (out of scope)
                Ctrl-->>View: Response
                View-->>User: "Xin lỗi, em chỉ hỗ trợ về OpenEvent..."
            else Not out of scope
                Agent->>Agent: isWeatherQuestion(userInput)
                alt Weather question
                    Agent->>Weather: getForecastNote(today, location)
                    Weather-->>Agent: Forecast note
                    Agent-->>ChatSvc: Weather response
                    ChatSvc-->>Ctrl: Response
                    Ctrl-->>View: Response
                    View-->>User: Weather info
                else Not weather question
                    Note over Agent: Step 5.3: Intent Classification
                    
                    Agent->>EmbedSvc: getEmbedding(userInput)
                    EmbedSvc-->>Agent: userVector (float[])
                    Agent->>VIC: classifyIntent(userInput, userVector)
                    VIC-->>Agent: Intent (e.g., ADD_EVENT)
                    
                    Note over Agent: Step 5.4: Call LLM
                    
                    Agent->>Agent: Add user message to context
                    Agent->>LLM: generateResponse(context)
                    LLM-->>Agent: AI Response Text + JSON Actions
                    
                    Note over Agent: Step 5.5: Parse Actions
                    
                    Agent->>Agent: Parse JSON → List<Action>
                    
                    alt Action == ADD_EVENT
                        Note over Agent: Step 6: Execute ADD_EVENT Action
                        
                        Agent->>Agent: Extract args (title, start, end, place, etc.)
                        
                        Note over Agent: Step 6.1-6.2: Validation
                        
                        Agent->>Agent: Validate (title != null, start < end)
                        
                        alt Validation failed
                            Agent-->>ChatSvc: Error message
                            ChatSvc-->>Ctrl: Response with error
                        else Validation OK
                            Note over Agent: Step 6.3: Find Place (Vector Search)
                            
                            Agent->>EmbedSvc: getEmbedding(placeName)
                            EmbedSvc-->>Agent: placeVector (float[])
                            Agent->>Qdrant: searchPlacesByVector(placeVector, limit=1)
                            Qdrant-->>Agent: searchResults [{id, place_id, score}]
                            
                            alt Found in Qdrant
                                Agent->>PlaceSvc: findPlaceById(place_id)
                                PlaceSvc->>DB: SELECT * FROM place WHERE id = ?
                                DB-->>PlaceSvc: Place entity
                                PlaceSvc-->>Agent: Optional<Place>
                            else Not found in Qdrant
                                Agent->>PlaceSvc: findPlaceByNameFlexible(placeName)
                                PlaceSvc->>DB: SELECT * FROM place WHERE name LIKE ?
                                DB-->>PlaceSvc: Place entity
                                PlaceSvc-->>Agent: Optional<Place>
                            end
                            
                            alt Place not found
                                Agent-->>ChatSvc: Error: "Không tìm thấy địa điểm"
                                ChatSvc-->>Ctrl: Response with error
                            else Place found
                                Note over Agent: Step 6.4: Check Time Conflict
                                
                                Agent->>EvSvc: isTimeConflict(start, end, places)
                                EvSvc->>DB: SELECT * FROM event<br/>WHERE time overlaps AND place matches
                                DB-->>EvSvc: Conflicting events
                                EvSvc-->>Agent: List<Event> conflicts
                                
                                alt Time conflict exists
                                    Agent-->>ChatSvc: Warning: "Trùng thời gian với..."
                                    ChatSvc-->>Ctrl: Response with warning
                                else No conflict
                                    Note over Agent: Step 6.5: Check Weather (Optional)
                                    
                                    Agent->>VIC: classifyWeather(userInput, userVector)
                                    VIC-->>Agent: weatherIntent (outdoor_activities?)
                                    
                                    alt weatherIntent == "outdoor_activities"
                                        Agent->>Weather: getForecastNote(start, "Da Nang")
                                        Weather-->>Agent: forecastNote
                                        
                                        alt forecastNote contains "rain"
                                            Agent->>Agent: pendingEvents.put(sessionId, event)
                                            Agent-->>ChatSvc: "🌦 Dự báo mưa. Tiếp tục?"
                                            ChatSvc-->>Ctrl: Response (confirmation needed)
                                            Ctrl-->>View: Response
                                            View-->>User: Weather warning + confirmation
                                        else No rain forecast
                                            Note over Agent: Continue to create event
                                        end
                                    else Not outdoor event
                                        Note over Agent: Continue to create event
                                    end
                                    
                                    alt Continue (no pending)
                                        Note over Agent: Step 6.6: Create EventItem
                                        
                                        Agent->>Agent: Create EventItem object<br/>(title, start, end, place, type, status=DRAFT)
                                        
                                        Note over Agent: Step 6.7: Call AgentEventService
                                        
                                        Agent->>AgentEvt: createEventByCustomer(userId, eventItem, orgId)
                                        
                                        Note over AgentEvt: Step 7.1: Get/Create Customer
                                        
                                        AgentEvt->>CustSvc: getOrCreateByUserId(userId)
                                        CustSvc->>CustRepo: findByAccount_AccountId(userId)
                                        CustRepo->>DB: SELECT * FROM customer WHERE account_id = ?
                                        DB-->>CustRepo: Customer (or null)
                                        CustRepo-->>CustSvc: Optional<Customer>
                                        
                                        alt Customer not found
                                            CustSvc->>CustRepo: save(new Customer)
                                            CustRepo->>DB: INSERT INTO customer
                                            DB-->>CustRepo: Customer saved
                                            CustRepo-->>CustSvc: Customer
                                        end
                                        CustSvc-->>AgentEvt: Customer
                                        
                                        Note over AgentEvt: Step 7.2: Create Event by Type
                                        
                                        AgentEvt->>AgentEvt: Create Event object<br/>(MusicEvent/WorkshopEvent/etc.)
                                        
                                        Note over AgentEvt: Step 7.3: Map EventItem → Event
                                        
                                        AgentEvt->>EventMapper: createEventFromRequest(eventItem, event)
                                        EventMapper-->>AgentEvt: Event mapped
                                        
                                        Note over AgentEvt: Step 7.4: Get/Create Host
                                        
                                        AgentEvt->>AgentEvt: Check if customer.hasHost()
                                        
                                        alt Customer has no Host
                                            AgentEvt->>HostSvc: findByCustomerId(customerId)
                                            HostSvc->>HostRepo: findByCustomer_CustomerId(customerId)
                                            HostRepo->>DB: SELECT * FROM host WHERE customer_id = ?
                                            DB-->>HostRepo: Host (or null)
                                            HostRepo-->>HostSvc: Optional<Host>
                                            
                                            alt Host not found
                                                HostSvc->>HostRepo: save(new Host)
                                                HostRepo->>DB: INSERT INTO host
                                                DB-->>HostRepo: Host saved
                                                HostRepo-->>HostSvc: Host
                                            end
                                            HostSvc-->>AgentEvt: Host
                                        end
                                        
                                        AgentEvt->>AgentEvt: event.setHost(host)
                                        
                                        Note over AgentEvt: Step 7.5: Set Organization (Optional)
                                        
                                        alt orgId != null
                                            AgentEvt->>OrgSvc: findById(orgId)
                                            OrgSvc->>OrgRepo: findById(orgId)
                                            OrgRepo->>DB: SELECT * FROM organization WHERE id = ?
                                            DB-->>OrgRepo: Organization
                                            OrgRepo-->>OrgSvc: Optional<Organization>
                                            OrgSvc-->>AgentEvt: Organization
                                            AgentEvt->>AgentEvt: event.setOrganization(org)
                                        end
                                        
                                        Note over AgentEvt: Step 7.6: Set Defaults
                                        
                                        AgentEvt->>AgentEvt: Set defaults<br/>(status=DRAFT, type=OTHERS, createdAt=now)
                                        
                                        Note over AgentEvt: Step 7.7: Save Event
                                        
                                        AgentEvt->>EventRepo: save(event)
                                        EventRepo->>DB: INSERT INTO event<br/>(title, description, starts_at, ends_at, host_id, ...)
                                        DB-->>EventRepo: Event (with id)
                                        EventRepo-->>AgentEvt: Event saved
                                        
                                        Note over AgentEvt: Step 7.8: Create Email Reminder
                                        
                                        AgentEvt->>AgentEvt: createOrUpdateEmailReminder(eventId, 5, userId)
                                        AgentEvt->>EmailRepo: findByEventIdAndUserId(eventId, userId)
                                        EmailRepo->>DB: SELECT * FROM email_reminder<br/>WHERE event_id = ? AND user_id = ?
                                        DB-->>EmailRepo: EmailReminder (or null)
                                        EmailRepo-->>AgentEvt: Optional<EmailReminder>
                                        
                                        alt Reminder not found
                                            AgentEvt->>EmailRepo: save(new EmailReminder)
                                            EmailRepo->>DB: INSERT INTO email_reminder<br/>(event_id, user_id, remind_minutes=5, sent=false)
                                            DB-->>EmailRepo: EmailReminder saved
                                        else Reminder exists
                                            AgentEvt->>EmailRepo: update(reminder)
                                            EmailRepo->>DB: UPDATE email_reminder SET remind_minutes = 5
                                            DB-->>EmailRepo: Updated
                                        end
                                        EmailRepo-->>AgentEvt: Reminder saved
                                        
                                        AgentEvt-->>Agent: Event created
                                        
                                        Agent->>Agent: systemResult.append("✅ Đã thêm sự kiện: " + title)
                                        Agent-->>ChatSvc: AI reply + event details
                                    end
                                end
                            end
                        end
                    end
                    
                    Note over ChatSvc: Step 8: Save AI Response
                    
                    ChatSvc->>ChatMsgRepo: save(aiMessage)
                    ChatMsgRepo->>DB: INSERT INTO chat_message<br/>(session_id, user_id, message, is_from_user=false)
                    DB-->>ChatMsgRepo: Saved
                    ChatMsgRepo-->>ChatSvc: AI message saved
                    
                    ChatSvc-->>Ctrl: ChatReply(response, shouldReload, timestamp)
                    
                    Note over Ctrl: Step 8.1: Translate Response
                    
                    alt userLanguage != VIETNAMESE
                        Ctrl->>Trans: translateAIResponse(response, userLanguage)
                        Trans-->>Ctrl: translatedResponse
                    else userLanguage == VIETNAMESE
                        Note over Ctrl: Use original response
                    end
                    
                    Note over Ctrl: Step 8.2: Validate Response
                    
                    Ctrl->>Security: validateAIResponse(translatedResponse)
                    Security-->>Ctrl: ValidationResult
                    
                    Ctrl-->>View: ResponseEntity<ChatReply>
                    View-->>User: Render AI text + event info
                    
                    alt shouldReload == true
                        View->>View: setTimeout(() => location.reload(), 1500)
                    end
                end
            end
        end
    end
```

---

## 📝 CÁC ĐIỂM QUAN TRỌNG ĐÃ BỔ SUNG

### 1. **EmbeddingService** 
- Tạo vector cho user input (intent classification)
- Tạo vector cho place name (vector search)

### 2. **PlaceService**
- Tìm place từ DB sau khi có place_id từ Qdrant
- Fallback: Tìm bằng tên nếu không tìm thấy trong Qdrant

### 3. **ChatMessageRepo**
- Load conversation history để build context
- Lưu user message và AI response

### 4. **Các Service trong AgentEventService**
- **CustomerService**: getOrCreateByUserId()
- **HostService**: findByCustomerId() hoặc tạo mới
- **OrganizationService**: findById() (nếu có orgId)
- **AIEventMapper**: Map EventItem → Event

### 5. **Build Context**
- Query DB để lấy 10 tin nhắn gần nhất
- Build context với system prompt + history

### 6. **Special Cases Check**
- isOutOfScope() - Kiểm tra câu hỏi ngoài phạm vi
- isWeatherQuestion() - Kiểm tra câu hỏi về thời tiết

### 7. **Weather Classification**
- classifyWeather() - Phân loại intent về thời tiết
- Chỉ gọi WeatherService nếu là outdoor_activities

### 8. **Response Translation**
- Dịch response về ngôn ngữ gốc của user
- Validate response trước khi trả về

### 9. **Pending Event Flow**
- Lưu event vào pendingEvents nếu có warning (mưa)
- Yêu cầu xác nhận từ user

### 10. **Error Handling**
- Alt blocks cho các trường hợp lỗi
- Rate limit, validation, conflicts, missing data

---

## 🔍 SO SÁNH VỚI BẢN GỐC

| Điểm | Bản gốc | Bản cải thiện |
|------|---------|---------------|
| Participants | 17 | 24 (thêm 7) |
| EmbeddingService | ❌ | ✅ |
| PlaceService | ❌ | ✅ |
| ChatMessageRepo | ❌ | ✅ |
| Services trong AgentEventService | ❌ (chỉ repo) | ✅ (CustomerService, HostService, etc.) |
| Build Context từ DB | ❌ | ✅ |
| Special Cases Check | ❌ | ✅ |
| Weather Classification | ❌ | ✅ |
| Response Translation | ❌ | ✅ |
| Error Handling | ⚠️ (minimal) | ✅ (detailed) |
| Pending Event Flow | ❌ | ✅ |

---

## ✅ KẾT LUẬN

**Sequence diagram bản gốc đã khá đầy đủ về luồng chính**, nhưng **thiếu nhiều chi tiết quan trọng** về:
- Các service trung gian (EmbeddingService, PlaceService, etc.)
- Error handling và các trường hợp đặc biệt
- Các bước query DB để build context
- Translation flow
- Chi tiết các bước trong AgentEventService

**Bản cải thiện này bổ sung đầy đủ các bước** và phản ánh chính xác hơn code thực tế.


