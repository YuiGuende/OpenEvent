# AI Workflow Documentation - OpenEvent System

## Tổng quan hệ thống AI

Hệ thống OpenEvent tích hợp nhiều tính năng AI để hỗ trợ người dùng quản lý sự kiện một cách thông minh và tự động. Các tính năng AI chính bao gồm:

1. **AI Chatbot Assistant** - Trợ lý AI đa ngôn ngữ
2. **Event Management AI** - Quản lý sự kiện thông minh
3. **Order Processing AI** - Xử lý đơn hàng tự động
4. **Translation Service** - Dịch thuật đa ngôn ngữ
5. **Weather Integration** - Tích hợp thời tiết
6. **Vector Search** - Tìm kiếm thông minh
7. **Email Reminder System** - Hệ thống nhắc nhở email

---

## 1. AI Chatbot Assistant Workflow

### 1.1 Kiến trúc tổng quan
```
User Input → Language Detection → Translation → AI Processing → Response Translation → User
```

### 1.2 Workflow chi tiết

#### Bước 1: Nhận input từ người dùng
- **Input**: Tin nhắn từ người dùng qua giao diện chatbot
- **Xử lý**: 
  - Lưu tin nhắn vào database (`ChatMessage`)
  - Tạo session mới nếu cần (`ChatSession`)

#### Bước 2: Phân tích ngôn ngữ và dịch thuật
- **Language Detection**: Sử dụng `LanguageDetectionService`
- **Translation**: 
  - Nếu không phải tiếng Việt → dịch sang tiếng Việt
  - Sử dụng `LibreTranslateService` với fallback URLs

#### Bước 3: Xử lý AI
- **EventAIAgent**: Xử lý logic chính
- **Intent Classification**: Phân loại ý định người dùng
- **Context Building**: Xây dựng context từ lịch sử chat

#### Bước 4: Tạo phản hồi
- **LLM Processing**: Sử dụng LLM để tạo phản hồi
- **Action Parsing**: Phân tích các hành động cần thực hiện
- **Response Generation**: Tạo phản hồi tự nhiên

#### Bước 5: Dịch và trả về
- **Response Translation**: Dịch phản hồi về ngôn ngữ người dùng
- **Database Storage**: Lưu phản hồi AI vào database
- **User Response**: Trả về cho người dùng

### 1.3 Các loại Intent được hỗ trợ
- `BUY_TICKET`: Mua vé sự kiện
- `QUERY_TICKET_INFO`: Xem thông tin vé
- `PROMPT_FREE_TIME`: Tìm thời gian rảnh
- `PROMPT_SUMMARY_TIME`: Tóm tắt lịch trình
- `PROMPT_SEND_EMAIL`: Gửi email nhắc nhở
- `CONFIRM_ORDER`: Xác nhận đơn hàng
- `CANCEL_ORDER`: Hủy đơn hàng

---

## 2. Event Management AI Workflow

### 2.1 Tạo sự kiện thông minh

#### Workflow tạo sự kiện:
```
User Request → Intent Detection → Information Extraction → Validation → Event Creation → Confirmation
```

#### Chi tiết các bước:

**Bước 1: Phân tích yêu cầu**
- Phát hiện intent "tạo sự kiện"
- Trích xuất thông tin: tên, thời gian, địa điểm, mô tả
- Sử dụng regex patterns để parse thông tin

**Bước 2: Validation**
- Kiểm tra thông tin đầy đủ
- Validate thời gian (start < end)
- Kiểm tra conflict với sự kiện khác
- Validate địa điểm tồn tại

**Bước 3: Weather Check**
- Nếu sự kiện ngoài trời → kiểm tra thời tiết
- Cảnh báo nếu có khả năng mưa
- Hỏi người dùng có muốn tiếp tục không

**Bước 4: Tạo sự kiện**
- Lưu vào database
- Tạo vector embedding cho search
- Sync với Qdrant vector database

### 2.2 Cập nhật sự kiện
- Tìm sự kiện theo ID hoặc tên
- Validate thông tin mới
- Cập nhật database
- Sync vector database

### 2.3 Xóa sự kiện
- Tìm sự kiện cần xóa
- Xác nhận với người dùng
- Xóa khỏi database
- Cleanup vector database

---

## 3. Order Processing AI Workflow

### 3.1 Quy trình mua vé tự động

#### Workflow mua vé:
```
Event Search → Ticket Selection → Info Collection → Order Confirmation → Payment → Completion
```

#### Chi tiết các bước:

**Bước 1: Tìm sự kiện**
- Vector search để tìm sự kiện phù hợp
- Chỉ hiển thị sự kiện có status PUBLIC
- Hiển thị thông tin sự kiện và các loại vé

**Bước 2: Chọn loại vé**
- Hiển thị danh sách ticket types
- Kiểm tra availability
- Người dùng chọn loại vé phù hợp

**Bước 3: Thu thập thông tin**
- Yêu cầu thông tin người tham gia:
  - Tên
  - Email
  - Số điện thoại
  - Tổ chức (tùy chọn)
- Validate thông tin

**Bước 4: Xác nhận đơn hàng**
- Hiển thị tóm tắt đơn hàng
- Yêu cầu xác nhận cuối cùng
- Phân tích intent xác nhận/hủy

**Bước 5: Tạo đơn hàng và thanh toán**
- Tạo Order trong database
- Tạo Payment link qua PayOS
- Gửi link thanh toán cho người dùng
- Tạo email reminder tự động

### 3.2 Quản lý trạng thái đơn hàng
- Sử dụng `PendingOrder` để track progress
- Các trạng thái: `SELECT_EVENT`, `SELECT_TICKET_TYPE`, `PROVIDE_INFO`, `CONFIRM_ORDER`
- Timeout và cleanup cho pending orders

---

## 4. Translation Service Workflow

### 4.1 Kiến trúc dịch thuật
```
Text Input → Language Detection → LibreTranslate API → Cached Result → Translated Text
```

### 4.2 Workflow chi tiết

**Bước 1: Language Detection**
- Sử dụng `LanguageDetectionService`
- Detect ngôn ngữ của input text
- Xác định ngôn ngữ đích

**Bước 2: Cache Check**
- Kiểm tra cache trước khi gọi API
- Cache key: `sourceLang:targetLang:textHash`
- Trả về cached result nếu có

**Bước 3: API Call**
- Gọi LibreTranslate API
- Fallback URLs nếu primary fail
- Retry mechanism

**Bước 4: Cache và Response**
- Cache kết quả dịch thuật
- Trả về translated text
- Log translation metrics

### 4.3 Supported Languages
- Vietnamese, English, Chinese, Japanese, Korean
- French, German, Spanish, Italian, Portuguese
- Russian, Arabic, Thai, Indonesian, Malay

---

## 5. Weather Integration Workflow

### 5.1 Weather Check Process
```
Event Creation → Weather Check → Forecast Analysis → User Notification
```

### 5.2 Chi tiết workflow

**Bước 1: Trigger Weather Check**
- Khi tạo sự kiện ngoài trời
- Intent classification: `outdoor_activities`
- Extract location từ event

**Bước 2: API Call**
- Gọi WeatherAPI.com
- Request forecast cho ngày sự kiện
- Parse JSON response

**Bước 3: Analysis**
- Kiểm tra điều kiện thời tiết
- Tính toán khả năng mưa
- Generate warning message

**Bước 4: User Notification**
- Hiển thị cảnh báo thời tiết
- Hỏi người dùng có muốn tiếp tục
- Lưu pending event nếu cần

---

## 6. Vector Search Workflow

### 6.1 Search Architecture
```
Query → Embedding → Vector Search → Ranking → Results
```

### 6.2 Workflow chi tiết

**Bước 1: Query Processing**
- Nhận search query từ user
- Preprocessing và normalization
- Language detection nếu cần

**Bước 2: Embedding Generation**
- Sử dụng `EmbeddingService`
- Call HuggingFace API
- Generate vector representation

**Bước 3: Vector Search**
- Search trong Qdrant vector database
- Cosine similarity calculation
- Ranking results by relevance

**Bước 4: Result Processing**
- Filter và format results
- Combine với database queries
- Return ranked results

### 6.3 Search Types
- **Event Search**: Tìm sự kiện theo tên, mô tả
- **Place Search**: Tìm địa điểm
- **Intent Classification**: Phân loại ý định người dùng

---

## 7. Email Reminder System Workflow

### 7.1 Reminder Creation
```
User Request → Event Identification → Reminder Setup → Scheduler → Email Delivery
```

### 7.2 Workflow chi tiết

**Bước 1: Request Processing**
- Parse reminder request từ user
- Extract event name và timing
- Validate user email

**Bước 2: Event Identification**
- Vector search để tìm sự kiện
- Fallback to upcoming events
- Confirm event details

**Bước 3: Reminder Setup**
- Calculate reminder time
- Save to `EmailReminder` table
- Setup scheduler job

**Bước 4: Email Delivery**
- `EmailReminderScheduler` chạy định kỳ
- Check upcoming reminders
- Send email via `EmailService`
- Update reminder status

### 7.3 Email Templates
- Event reminder với thông tin chi tiết
- Payment confirmation
- Event updates
- Custom messages

---

## 8. Security và Rate Limiting

### 8.1 AI Security Features
- **Rate Limiting**: Giới hạn số lượng requests
- **Input Validation**: Validate user input
- **Session Management**: Quản lý session an toàn
- **Error Handling**: Xử lý lỗi graceful

### 8.2 Rate Limiting Workflow
```
Request → Rate Limit Check → Security Validation → Processing → Response
```

---

## 9. Error Handling và Monitoring

### 9.1 Error Types
- **API Failures**: LibreTranslate, WeatherAPI, HuggingFace
- **Database Errors**: Connection, transaction failures
- **Validation Errors**: Input validation failures
- **Timeout Errors**: Request timeout

### 9.2 Error Handling Strategy
- **Graceful Degradation**: Fallback mechanisms
- **Retry Logic**: Automatic retry với exponential backoff
- **Logging**: Comprehensive error logging
- **User Feedback**: Friendly error messages

---

## 10. Performance Optimization

### 10.1 Caching Strategy
- **Translation Cache**: Cache dịch thuật
- **Embedding Cache**: Cache vector embeddings
- **Session Cache**: Cache conversation context
- **Weather Cache**: Cache weather forecasts

### 10.2 Async Processing
- **Async Translation**: Non-blocking translation
- **Background Jobs**: Email sending, data sync
- **Batch Processing**: Bulk operations

---

## 11. Monitoring và Analytics

### 11.1 Metrics to Track
- **Response Time**: AI response latency
- **Success Rate**: Successful operations
- **Error Rate**: Failed operations
- **User Engagement**: Chat session metrics

### 11.2 Logging Strategy
- **Structured Logging**: JSON format logs
- **Log Levels**: DEBUG, INFO, WARN, ERROR
- **Context Information**: User ID, Session ID, Request ID

---

## 12. Deployment và Scaling

### 12.1 Infrastructure Requirements
- **Database**: MySQL cho persistent data
- **Vector Database**: Qdrant cho vector search
- **Cache**: Redis cho caching
- **External APIs**: LibreTranslate, WeatherAPI, HuggingFace

### 12.2 Scaling Considerations
- **Horizontal Scaling**: Multiple service instances
- **Load Balancing**: Distribute requests
- **Database Scaling**: Read replicas, connection pooling
- **Cache Scaling**: Redis cluster

---

## Kết luận

Hệ thống AI của OpenEvent được thiết kế với kiến trúc modular, có khả năng mở rộng và xử lý lỗi tốt. Các workflow được tối ưu hóa để đảm bảo trải nghiệm người dùng mượt mà và hiệu quả cao.

### Điểm mạnh:
- ✅ Đa ngôn ngữ support
- ✅ Vector search thông minh
- ✅ Tích hợp thời tiết
- ✅ Xử lý đơn hàng tự động
- ✅ Email reminder system
- ✅ Error handling tốt
- ✅ Caching strategy hiệu quả

### Cơ hội cải thiện:
- 🔄 Real-time notifications
- 🔄 Advanced analytics
- 🔄 Machine learning recommendations
- 🔄 Voice interface support
- 🔄 Mobile app integration




