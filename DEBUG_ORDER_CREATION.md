# 🔍 DEBUG ORDER CREATION - Hướng dẫn Debug

## 🎯 **MỤC TIÊU:**
Tìm nguyên nhân tại sao Order không được lưu vào database mặc dù AI response "thành công".

---

## 🚀 **CÁC BƯỚC DEBUG:**

### **BƯỚC 1: Restart Application với Debug Logs**
```bash
# Stop app hiện tại
Ctrl + C

# Start lại với debug logs
mvn spring-boot:run
```

### **BƯỚC 2: Test Order Creation**
```bash
# Trong Postman hoặc curl
POST http://localhost:8080/api/ai/chat
{
    "message": "Mua vé Music Night",
    "userId": 1
}

# Tiếp tục flow:
# 1. "Chọn vé VIP"
# 2. "Tên: Test, Email: test@test.com, SĐT: 0123456789"  
# 3. "Có"
```

### **BƯỚC 3: Xem Debug Logs**
Tìm các log messages sau trong console:

#### **✅ Logs thành công:**
```
🔍 DEBUG: Starting confirmOrder for userId: 1
🔍 DEBUG: Found pending order: YES
🔍 DEBUG: Pending order details - Event: Music Night, TicketType: VIP, Participant: Test
🔍 DEBUG: Customer found - customerId: 1, email: test@example.com
🔍 DEBUG: Creating order with OrderService...
🔍 DEBUG: Event found - title: Music Night, status: PUBLIC
🔍 DEBUG: Ticket type found - name: VIP, price: 500000, available: 10
🔍 DEBUG: Tickets reserved successfully
🔍 DEBUG: Saving order to database...
✅ DEBUG: Order saved successfully - orderId: 12345, status: PENDING
✅ DEBUG: Payment created successfully - paymentId: 1, checkoutUrl: https://...
✅ DEBUG: Order creation completed successfully
```

#### **❌ Logs lỗi (tìm nguyên nhân):**
```
❌ DEBUG: Customer not found for userId: 1
❌ DEBUG: Event not found: 67
❌ DEBUG: Ticket type not found: 101
❌ DEBUG: Cannot purchase tickets - available: 0
❌ DEBUG: Exception in createOrderWithTicketTypes: ...
❌ DEBUG: Exception in createPaymentLinkForOrder: ...
```

### **BƯỚC 4: Kiểm tra Database**
```bash
# Chạy script SQL debug
mysql -u username -p database_name < debug_database_test.sql
```

**Hoặc manual queries:**
```sql
-- Check customers
SELECT * FROM customers;
SELECT * FROM accounts WHERE account_id = 1;

-- Check events  
SELECT * FROM events WHERE title LIKE '%Music Night%';

-- Check ticket types
SELECT * FROM ticket_type;

-- Check orders (should be empty nếu có lỗi)
SELECT * FROM orders;
SELECT * FROM payments;
```

---

## 🔍 **CÁC NGUYÊN NHÂN CÓ THỂ:**

### **1. ❌ Customer không tồn tại:**
```
Log: "❌ DEBUG: Customer not found for userId: 1"
Fix: Tạo customer với account_id = 1
```

### **2. ❌ Event không tồn tại:**
```
Log: "❌ DEBUG: Event not found: 67"  
Fix: Kiểm tra events table có event "Music Night" không
```

### **3. ❌ Ticket Type không tồn tại:**
```
Log: "❌ DEBUG: Ticket type not found: 101"
Fix: Kiểm tra ticket_type table có ticket cho event không
```

### **4. ❌ Tickets hết hàng:**
```
Log: "❌ DEBUG: Cannot purchase tickets - available: 0"
Fix: Update available_quantity > 0
```

### **5. ❌ PayOS API Error:**
```
Log: "❌ DEBUG: Exception in createPaymentLinkForOrder"
Fix: Kiểm tra PayOS configuration
```

### **6. ❌ Database Connection:**
```
Log: "❌ DEBUG: Exception in createOrderWithTicketTypes"
Fix: Kiểm tra database connection
```

---

## 📊 **EXPECTED RESULTS:**

### **✅ Nếu thành công:**
- Database `orders` table có 1 record mới
- Database `payments` table có 1 record mới  
- Postman response có payment link
- Console logs hiển thị "✅ DEBUG: Order creation completed successfully"

### **❌ Nếu thất bại:**
- Database tables vẫn trống
- Console logs hiển thị exception details
- Có thể AI vẫn response "thành công" (bug trong error handling)

---

## 🛠️ **QUICK FIXES:**

### **Fix 1: Tạo test data**
```sql
-- Tạo customer test
INSERT INTO accounts (account_id, email, password_hash, role) VALUES (1, 'test@test.com', 'hash', 'CUSTOMER');
INSERT INTO customers (customer_id, account_id, phone_number, points) VALUES (1, 1, '0123456789', 0);

-- Tạo event test  
INSERT INTO events (id, title, status, starts_at, ends_at) VALUES (67, 'Music Night', 'PUBLIC', '2024-12-01 19:00:00', '2024-12-01 22:00:00');

-- Tạo ticket type test
INSERT INTO ticket_type (ticket_type_id, name, final_price, available_quantity, event_id) VALUES (101, 'VIP', 500000, 10, 67);
```

### **Fix 2: Check PayOS Config**
```properties
# application.properties
payos.client-id=your-client-id
payos.api-key=your-api-key
payos.checksum-key=your-checksum-key
```

---

## 📝 **REPORT BUG:**

Sau khi debug, báo cáo:

1. **Console logs** (copy paste)
2. **Database status** (từ debug_database_test.sql)
3. **Error message** (nếu có)
4. **Steps to reproduce**

---

**🎯 Mục tiêu: Tìm được chính xác bước nào fail và fix nó!**






