# Hướng Dẫn Test WebSocket Event Chat

## 📋 Yêu Cầu Trước Khi Test

### 1. Chuẩn Bị Dữ Liệu

Bạn cần có:
- **1 Host** (tài khoản đã đăng ký làm Host)
- **1 Customer** (tài khoản Customer)
- **1 Event** (do Host tạo)
- **1 Volunteer Application** (Customer đã apply và được Host approve)

### 2. Kiểm Tra Database

#### A. Kiểm tra Host:
```sql
-- Kiểm tra user có phải host không
SELECT u.user_id, u.name, h.host_id, h.host_name 
FROM user u 
LEFT JOIN host h ON h.user_id = u.user_id 
WHERE u.user_id = <HOST_USER_ID>;
```

#### B. Kiểm tra Event:
```sql
-- Kiểm tra event có host chưa
SELECT e.id, e.title, e.host_id, h.host_name 
FROM event e 
LEFT JOIN host h ON h.host_id = e.host_id 
WHERE e.id = <EVENT_ID>;
```

#### C. Kiểm tra Volunteer Application:
```sql
-- Kiểm tra volunteer đã được approve chưa
SELECT va.id, va.customer_id, va.event_id, va.status, c.user_id
FROM volunteer_application va
JOIN customer c ON c.customer_id = va.customer_id
WHERE va.event_id = <EVENT_ID> 
  AND va.status = 'APPROVED'
  AND c.user_id = <VOLUNTEER_USER_ID>;
```

#### D. Nếu chưa có Volunteer Application, tạo mới:
```sql
-- Tìm customer_id từ user_id
SELECT customer_id FROM customer WHERE user_id = <VOLUNTEER_USER_ID>;

-- Tạo volunteer application (thay <CUSTOMER_ID> và <EVENT_ID>)
INSERT INTO volunteer_application (customer_id, event_id, status, created_at)
VALUES (<CUSTOMER_ID>, <EVENT_ID>, 'APPROVED', NOW());
```

---

## 🧪 CÁCH TEST BẰNG 2 TRÌNH DUYỆT

### Bước 1: Khởi Động Server

```bash
# Chạy Spring Boot application
mvn spring-boot:run
# hoặc
./mvnw spring-boot:run
```

Server sẽ chạy tại: `http://localhost:8080`

### Bước 2: Mở 2 Trình Duyệt

#### **Trình Duyệt A - HOST:**
1. Mở trình duyệt (Chrome/Firefox/Edge)
2. Mở Developer Tools (F12)
3. Vào tab **Console** và **Network**
4. Đăng nhập với tài khoản **Host**
5. Truy cập: `http://localhost:8080/event-chat?eventId=<EVENT_ID>`
   - Ví dụ: `http://localhost:8080/event-chat?eventId=1`

#### **Trình Duyệt B - VOLUNTEER:**
1. Mở trình duyệt khác (hoặc chế độ ẩn danh)
2. Mở Developer Tools (F12)
3. Vào tab **Console** và **Network**
4. Đăng nhập với tài khoản **Customer** (đã được approve làm volunteer)
5. Truy cập: `http://localhost:8080/event-chat?eventId=<EVENT_ID>`
   - Ví dụ: `http://localhost:8080/event-chat?eventId=1`

### Bước 3: Kiểm Tra WebSocket Connection

#### **A. Kiểm tra trong Console tab:**
Trong **Console tab** (không phải Network tab) của cả 2 trình duyệt, bạn sẽ thấy:
```
Connected to event chat WebSocket
Subscribing to room: /queue/event-chat/rooms/<ROOM_ID>
rooms length = 1 [...]
```

**Lưu ý quan trọng:** Log `Subscribing to room: /queue/event-chat/rooms/<ROOM_ID>` chỉ xuất hiện khi:
- ✅ Đã có room trong database (đã gửi message ít nhất 1 lần)
- ✅ Room được load từ API và tự động chọn (room đầu tiên)
- ✅ Hoặc bạn click vào một room trong sidebar

#### **B. Kiểm tra trong Network tab:**
Trong **Network tab** → chọn **WS** (WebSocket) → click vào connection `/ws`:

Bạn sẽ thấy các frames:
1. **CONNECTED frame:**
   ```
   CONNECTED
   version:1.1
   heart-beat:0,0
   user-name:3  ← Đây là userId của bạn
   ```
   → Chứng tỏ WebSocket đã kết nối thành công

2. **SUBSCRIBE frame:**
   ```
   SUBSCRIBE
   id:sub-0
   destination:/queue/event-chat/rooms/1  ← Đây là room đã subscribe
   ```
   → Chứng tỏ đã subscribe vào room thành công

**Nếu thấy SUBSCRIBE frame trong Network tab nhưng không thấy log trong Console:**
- ✅ Code đã chạy đúng! Log có thể bị clear hoặc filter
- ✅ WebSocket connection và subscription đã thành công
- Kiểm tra Console tab có filter nào không (All levels, Errors, Warnings, Info)

**Nếu chưa thấy log "Subscribing to room" trong Console:**
- Đây là **BÌNH THƯỜNG** nếu chưa có room nào (chưa gửi message lần đầu)
- Room sẽ được tạo tự động khi bạn gửi message đầu tiên
- Sau khi gửi message, refresh trang và bạn sẽ thấy log "Subscribing to room"

**Kiểm tra:**
- ✅ Xem có lỗi trong Console không
- ✅ Kiểm tra Network tab → WS (WebSocket) → xem connection có thành công không
- ✅ Kiểm tra Console có log `rooms length = 0` hay `rooms length = 1` (nếu có room)
- ✅ Kiểm tra Console filter settings (đảm bảo "All levels" được chọn)

### Bước 4: Test Gửi Message

**Lưu ý:** Nếu chưa có room (chưa gửi message lần đầu), bạn sẽ thấy "Chưa có cuộc trò chuyện nào" trong sidebar. Điều này là bình thường!

#### **Từ HOST (Trình Duyệt A):**
1. **Nếu chưa có room:** Bạn vẫn có thể gửi message! Nhập message và nhấn Gửi, room sẽ được tạo tự động.
2. **Nếu đã có room:** Chọn room trong sidebar
3. Nhập message: "Xin chào volunteer!"
4. Nhấn **Gửi** hoặc **Enter**

**Sau khi gửi message đầu tiên:**
- Room sẽ được tạo tự động trong database
- Refresh trang (F5) để thấy room trong sidebar
- Log "Subscribing to room" sẽ xuất hiện khi room được load và chọn tự động

#### **Kiểm Tra:**
- ✅ Message xuất hiện ngay trong chat của HOST
- ✅ Message xuất hiện ngay trong chat của VOLUNTEER (real-time)
- ✅ Trong Console của VOLUNTEER, bạn sẽ thấy:
  ```
  Received WebSocket message: {"roomId":1,"eventId":1,"messageId":1,"senderUserId":2,"recipientUserId":3,"body":"Xin chào volunteer!","timestamp":"2024-..."}
  ```

#### **Từ VOLUNTEER (Trình Duyệt B):**
1. Nhập message: "Chào host, tôi sẵn sàng!"
2. Nhấn **Gửi**

#### **Kiểm Tra:**
- ✅ Message xuất hiện ngay trong chat của VOLUNTEER
- ✅ Message xuất hiện ngay trong chat của HOST (real-time)

### Bước 5: Kiểm Tra Database

```sql
-- Kiểm tra room đã được tạo
SELECT * FROM event_chat_room WHERE event_id = <EVENT_ID>;

-- Kiểm tra messages đã được lưu
SELECT * FROM event_chat_message 
WHERE room_id = <ROOM_ID> 
ORDER BY timestamp DESC;
```

---

## 🔍 KIỂM TRA CHI TIẾT

### 1. Kiểm Tra WebSocket Connection

Trong Console, chạy:
```javascript
// Kiểm tra STOMP client
console.log('STOMP connected:', stompClient && stompClient.connected);
console.log('Current room ID:', currentRoomId);
console.log('Current user ID:', currentUserId);
```

### 2. Kiểm Tra API Endpoints

#### A. Lấy danh sách rooms:
```bash
# Trong terminal hoặc Postman
curl -X GET "http://localhost:8080/api/event-chat/rooms/1" \
  -H "Cookie: JSESSIONID=<YOUR_SESSION_ID>" \
  -H "Content-Type: application/json"
```

**Response mong đợi:**
```json
[
  {
    "id": 1,
    "createdAt": "2024-01-15T10:30:00",
    "host": {
      "userId": 2,
      "name": "Host Name",
      "email": "host@example.com"
    },
    "volunteer": {
      "userId": 3,
      "name": "Volunteer Name",
      "email": "volunteer@example.com"
    }
  }
]
```

#### B. Lấy message history:
```bash
curl -X GET "http://localhost:8080/api/event-chat/rooms/1/messages?page=0&size=20" \
  -H "Cookie: JSESSIONID=<YOUR_SESSION_ID>" \
  -H "Content-Type: application/json"
```

### 3. Kiểm Tra Logs Server

Trong console của Spring Boot, bạn sẽ thấy:
```
Sending chat message DTO: roomId=1, eventId=1, messageId=1, senderUserId=2, recipientUserId=3, body=Xin chào volunteer!, timestamp=2024-...
Sent chat message to room 1 via destination /queue/event-chat/rooms/1
```

---

## 🐛 XỬ LÝ LỖI THƯỜNG GẶP

### Lỗi 1: "Not authenticated"
**Nguyên nhân:** Chưa đăng nhập hoặc session hết hạn  
**Giải pháp:** Đăng nhập lại

### Lỗi 2: "Volunteer is not approved for this event"
**Nguyên nhân:** Volunteer chưa được approve  
**Giải pháp:** 
```sql
UPDATE volunteer_application 
SET status = 'APPROVED' 
WHERE customer_id = <CUSTOMER_ID> AND event_id = <EVENT_ID>;
```

### Lỗi 3: "Event host not found"
**Nguyên nhân:** Event chưa có host  
**Giải pháp:** Kiểm tra và gán host cho event

### Lỗi 4: WebSocket không kết nối
**Nguyên nhân:** 
- Server chưa chạy
- Port bị chặn
- CORS issue

**Giải pháp:**
- Kiểm tra server đang chạy
- Kiểm tra Console có lỗi gì không
- Kiểm tra Network tab → WS connection

### Lỗi 5: Message không hiển thị real-time
**Nguyên nhân:** 
- Chưa subscribe đúng room
- WebSocket connection bị ngắt

**Giải pháp:**
- Refresh trang
- Kiểm tra Console xem có subscribe đúng room không
- Kiểm tra `currentRoomSubscription` trong Console

---

## 📊 TEST CASE CHECKLIST

- [ ] Host có thể truy cập `/event-chat?eventId=<ID>`
- [ ] Volunteer có thể truy cập `/event-chat?eventId=<ID>`
- [ ] WebSocket connection thành công (cả 2 bên)
- [ ] Room được tạo tự động khi gửi message đầu tiên
- [ ] Host gửi message → Volunteer nhận được real-time
- [ ] Volunteer gửi message → Host nhận được real-time
- [ ] Message được lưu vào database
- [ ] Message history load đúng khi chọn room
- [ ] JSON response không có circular references
- [ ] Không có lỗi trong Console
- [ ] Không có lỗi trong Server logs

---

## 🎯 TEST NÂNG CAO

### Test Multiple Rooms:
1. Tạo nhiều volunteer applications cho cùng 1 event
2. Host chat với nhiều volunteers khác nhau
3. Kiểm tra mỗi room hoạt động độc lập

### Test Concurrent Messages:
1. Cả 2 bên gửi message cùng lúc
2. Kiểm tra không bị mất message
3. Kiểm tra thứ tự message đúng

### Test Reconnection:
1. Ngắt kết nối mạng
2. Kết nối lại
3. Kiểm tra message history vẫn load đúng

---

## 📝 NOTES

- **Room được tạo tự động** khi gửi message đầu tiên giữa host và volunteer
- **Message được broadcast** đến tất cả clients đang subscribe vào room đó
- **JSON response** sử dụng DTO, không có circular references
- **WebSocket destination:** `/queue/event-chat/rooms/{roomId}`

---

**Chúc bạn test thành công! 🎉**

