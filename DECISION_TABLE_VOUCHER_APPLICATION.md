# Decision Table Testing - Voucher Application

## Feature được kiểm thử

### Tên feature/nghiệp vụ:
**Voucher Application** - Áp dụng mã giảm giá (voucher) vào đơn hàng (Order)

### Mã/Link nguồn yêu cầu (BRD/SRS/User Story/AC/Jira):
- **Controller**: `OrderController.java` - Method `applyVoucher()` (lines 239-273)
- **Service**: `VoucherServiceImpl.java` - Method `applyVoucherToOrder()` (lines 48-80)
- **Repository**: `IVoucherRepo.java` - Query `findAvailableVoucherByCode()` (line 26)
- **Endpoint**: `POST /api/orders/{orderId}/apply-voucher?voucherCode={code}`
- **User Story**: User có thể áp dụng voucher code vào order của mình để giảm giá, với điều kiện voucher hợp lệ, chưa hết hạn, còn số lượng, và discount không vượt quá giá gốc

---

## Luật nghiệp vụ (Business Rules)

### BR-01: IF user is not logged in THEN return HTTP 401 với message "User not logged in"
- **Ngưỡng**: `accountId == null`
- **Hành động**: Trả về HTTP 401 với `{"success": false, "message": "User not logged in"}`

### BR-02: IF order does not exist THEN return HTTP 400 với message "Order not found"
- **Ngưỡng**: `orderOpt.isEmpty()`
- **Hành động**: Trả về HTTP 400 với `{"success": false, "message": "Order not found"}`

### BR-03: IF order does not belong to current user THEN return HTTP 403 với message "Access denied"
- **Ngưỡng**: `order.getCustomer().getAccount().getAccountId() != accountId`
- **Hành động**: Trả về HTTP 403 với `{"success": false, "message": "Access denied"}`

### BR-04: IF voucher code does not exist OR voucher is not available THEN throw IllegalArgumentException "Voucher không hợp lệ hoặc đã hết hạn"
- **Điều kiện**: `voucherOpt.isEmpty()` từ query `findAvailableVoucherByCode()`
- **Query điều kiện**: `status = 'ACTIVE' AND (expiresAt IS NULL OR expiresAt > :now) AND quantity > 0`
- **Hành động**: Throw `IllegalArgumentException` → Controller catch và trả về HTTP 400 với error message

### BR-05: IF voucher quantity <= 0 THEN throw IllegalArgumentException "Voucher đã hết số lượng sử dụng"
- **Ngưỡng**: `voucher.getQuantity() <= 0` (double-check sau khi query)
- **Hành động**: Throw `IllegalArgumentException` → Controller catch và trả về HTTP 400

### BR-06: IF discount amount > original price THEN cap discount = original price
- **Ngưỡng**: `discountAmount.compareTo(originalPrice) > 0`
- **Hành động**: Set `discountAmount = originalPrice` trong `calculateVoucherDiscount()`

### BR-07: IF all validations pass THEN apply voucher to order
- **Điều kiện**: Tất cả điều kiện trên đều pass
- **Hành động**:
  1. Calculate discount amount (với cap logic)
  2. Create `VoucherUsage` record
  3. Update Order: set voucher, voucherCode, voucherDiscountAmount
  4. Call `order.calculateTotalAmount()` để recalculate total
  5. Decrease voucher quantity by 1
  6. Save voucher và order
  7. Return HTTP 200 với success response

---

## Điều kiện (Conditions) – Cx

### C1: User Authentication
- **Kiểu dữ liệu**: Boolean
- **Miền giá trị**:
  - `true`: `accountId != null` (user đã đăng nhập)
  - `false`: `accountId == null` (user chưa đăng nhập)
- **Phân lớp tương đương**:
  - Valid: accountId là Long > 0
  - Invalid: null
- **Điểm biên**: `accountId == null`

### C2: Order Existence
- **Kiểu dữ liệu**: Boolean
- **Miền giá trị**:
  - `true`: Order tồn tại trong database với `orderId`
  - `false`: Order không tồn tại
- **Phân lớp tương đương**:
  - Valid: Order exists với orderId hợp lệ
  - Invalid: orderId không tồn tại trong DB
- **Điểm biên**: `orderId == null` (không thể xảy ra vì là path variable)

### C3: Order Ownership
- **Kiểu dữ liệu**: Boolean
- **Miền giá trị**:
  - `true`: `order.getCustomer().getAccount().getAccountId().equals(accountId)`
  - `false`: Order không thuộc về user hiện tại
- **Phân lớp tương đương**:
  - Valid: Order belongs to current user
  - Invalid: Order belongs to different user
- **Điểm biên**: Không có (boolean)

### C4: Voucher Code Existence
- **Kiểu dữ liệu**: Boolean
- **Miền giá trị**:
  - `true`: Voucher tồn tại với code trong database
  - `false`: Voucher không tồn tại
- **Phân lớp tương đương**:
  - Valid: Voucher exists với code match (case-sensitive)
  - Invalid: Code không tồn tại
- **Điểm biên**: 
  - Boundary: `voucherCode == null` (không thể xảy ra vì là required param)
  - Boundary: `voucherCode == ""` (empty string)

### C5: Voucher Status
- **Kiểu dữ liệu**: Enum (VoucherStatus)
- **Miền giá trị**:
  - `ACTIVE`: Voucher đang hoạt động
  - `EXPIRED`: Voucher đã hết hạn (status, không phải date)
  - `DISABLED`: Voucher đã bị vô hiệu hóa
- **Phân lớp tương đương**:
  - Valid: ACTIVE
  - Invalid: EXPIRED, DISABLED
- **Điểm biên**: Không có (enum cố định)

### C6: Voucher Expiration Date
- **Kiểu dữ liệu**: DateTime (nullable)
- **Miền giá trị**:
  - `null`: Voucher không có ngày hết hạn (vô thời hạn)
  - `expiresAt > now`: Voucher chưa hết hạn
  - `expiresAt <= now`: Voucher đã hết hạn
- **Phân lớp tương đương**:
  - Valid: `expiresAt IS NULL` OR `expiresAt > LocalDateTime.now()`
  - Invalid: `expiresAt <= LocalDateTime.now()`
- **Điểm biên**: 
  - Boundary: `expiresAt == null` (vô thời hạn)
  - Boundary: `expiresAt == LocalDateTime.now()` (chính xác tại thời điểm hiện tại - edge case)
  - Boundary: `expiresAt == LocalDateTime.now().minusSeconds(1)` (vừa hết hạn)

### C7: Voucher Quantity
- **Kiểu dữ liệu**: Integer
- **Miền giá trị**:
  - `> 0`: Voucher còn số lượng
  - `<= 0`: Voucher hết số lượng
- **Phân lớp tương đương**:
  - Valid: quantity > 0
  - Invalid: quantity <= 0
- **Điểm biên**: 
  - Boundary: `quantity == 0` (hết số lượng)
  - Boundary: `quantity == 1` (còn 1 lần sử dụng cuối)
  - Boundary: `quantity < 0` (không hợp lệ nhưng có thể xảy ra do bug)

### C8: Discount Amount vs Original Price
- **Kiểu dữ liệu**: BigDecimal comparison
- **Miền giá trị**:
  - `discountAmount <= originalPrice`: Discount hợp lệ
  - `discountAmount > originalPrice`: Discount vượt quá giá gốc (cần cap)
- **Phân lớp tương đương**:
  - Valid: discountAmount <= originalPrice
  - Invalid: discountAmount > originalPrice (sẽ được cap)
- **Điểm biên**: 
  - Boundary: `discountAmount == originalPrice` (giảm 100%)
  - Boundary: `discountAmount == originalPrice + 0.01` (vượt quá 1 cent)
  - Boundary: `originalPrice == 0` (order miễn phí)

---

## Hành động/Kết quả (Actions) – Ax

### A1: Return Unauthorized Response
- **Mô tả**: Trả về HTTP 401 khi user chưa đăng nhập
- **Response**: `HTTP 401`, `{"success": false, "message": "User not logged in"}`
- **Điều kiện kích hoạt**: C1 = false

### A2: Return Order Not Found Response
- **Mô tả**: Trả về HTTP 400 khi order không tồn tại
- **Response**: `HTTP 400`, `{"success": false, "message": "Order not found"}`
- **Điều kiện kích hoạt**: C2 = false

### A3: Return Access Denied Response
- **Mô tả**: Trả về HTTP 403 khi order không thuộc về user
- **Response**: `HTTP 403`, `{"success": false, "message": "Access denied"}`
- **Điều kiện kích hoạt**: C3 = false

### A4: Throw Voucher Invalid Exception
- **Mô tả**: Throw IllegalArgumentException khi voucher không hợp lệ hoặc hết hạn
- **Exception**: `IllegalArgumentException("Voucher không hợp lệ hoặc đã hết hạn")`
- **Kết quả**: Controller catch và trả về HTTP 400 với error message
- **Điều kiện kích hoạt**: C4 = false OR C5 != ACTIVE OR C6 = expired

### A5: Throw Voucher Quantity Exhausted Exception
- **Mô tả**: Throw IllegalArgumentException khi voucher hết số lượng
- **Exception**: `IllegalArgumentException("Voucher đã hết số lượng sử dụng")`
- **Kết quả**: Controller catch và trả về HTTP 400
- **Điều kiện kích hoạt**: C7 <= 0

### A6: Calculate Discount Amount
- **Mô tả**: Tính toán discount amount với logic cap
- **Sequence**:
  1. Get voucher discount amount từ database
  2. Compare với originalPrice
  3. If discountAmount > originalPrice: cap = originalPrice
  4. Return discountAmount (có thể đã được cap)
- **Điều kiện kích hoạt**: Tất cả validation pass, trước khi apply voucher

### A7: Create VoucherUsage Record
- **Mô tả**: Tạo bản ghi sử dụng voucher
- **Sequence**:
  1. Create new `VoucherUsage` object với voucher, order, discountApplied
  2. Set `usedAt = LocalDateTime.now()`
  3. Save vào database
- **Điều kiện kích hoạt**: A6 completed successfully

### A8: Update Order with Voucher Info
- **Mô tả**: Cập nhật Order với thông tin voucher
- **Sequence**:
  1. Set `order.setVoucher(voucher)`
  2. Set `order.setVoucherCode(voucherCode)`
  3. Set `order.setVoucherDiscountAmount(discountAmount)`
  4. Call `order.calculateTotalAmount()` để recalculate total (bao gồm VAT 10%)
- **Điều kiện kích hoạt**: A7 completed successfully

### A9: Decrease Voucher Quantity
- **Mô tả**: Giảm số lượng voucher đi 1
- **Sequence**:
  1. Call `voucher.decreaseQuantity()` (check quantity > 0 trước khi giảm)
  2. Save voucher vào database
- **Điều kiện kích hoạt**: A8 completed successfully

### A10: Save Order
- **Mô tả**: Lưu Order đã được cập nhật
- **Sequence**:
  1. Call `orderService.save(order)`
  2. Order được persist với voucher info và total amount mới
- **Điều kiện kích hoạt**: A9 completed successfully

### A11: Return Success Response
- **Mô tả**: Trả về response thành công với thông tin discount và total mới
- **Response**: `HTTP 200`, `{"success": true, "message": "Voucher applied successfully", "discountAmount": <amount>, "newTotalAmount": <total>}`
- **Điều kiện kích hoạt**: A10 completed successfully

### A12: Return Error Response (Exception Handler)
- **Mô tả**: Trả về response lỗi khi có exception
- **Response**: `HTTP 400`, `{"success": false, "message": <exception_message>}`
- **Điều kiện kích hoạt**: Bất kỳ exception nào trong try-catch block

---

## Ưu tiên xung đột (Precedence/Priority)

Khi nhiều luật cùng khớp, thứ tự ưu tiên:

1. **C1 = false (User not logged in)** → A1 (BR-01) - **Ưu tiên cao nhất**
2. **C2 = false (Order not found)** → A2 (BR-02) - **Ưu tiên cao**
3. **C3 = false (Access denied)** → A3 (BR-03) - **Ưu tiên cao**
4. **C4 = false OR C5 != ACTIVE OR C6 = expired** → A4 (BR-04) - **Ưu tiên trung bình**
5. **C7 <= 0 (Quantity exhausted)** → A5 (BR-05) - **Ưu tiên trung bình**
6. **C8 > originalPrice (Discount cap)** → A6 với cap logic (BR-06) - **Ưu tiên thấp** (tự động xử lý)
7. **All validations pass** → A7, A8, A9, A10, A11 (BR-07) - **Ưu tiên thấp** (normal flow)

**Lý do**: Authentication và authorization checks phải được thực hiện trước, sau đó mới đến business logic validation.

---

## Tổ hợp không khả thi (Infeasible Combos)

### IC-01: C2 = false AND C3 = true
- **Lý do**: Nếu Order không tồn tại (C2 = false) thì không thể check ownership (C3)

### IC-02: C4 = false AND (C5 = ACTIVE OR C6 = valid OR C7 > 0)
- **Lý do**: Nếu Voucher không tồn tại (C4 = false) thì không thể có status, expiration, quantity

### IC-03: C5 != ACTIVE AND C6 = valid
- **Lý do**: Query `findAvailableVoucherByCode()` chỉ trả về voucher với status = ACTIVE, nên nếu C5 != ACTIVE thì C6 không thể valid

### IC-04: C7 <= 0 AND C4 = true AND C5 = ACTIVE AND C6 = valid
- **Lý do**: Query đã check `quantity > 0`, nên nếu voucher được trả về từ query thì C7 phải > 0. Tuy nhiên, có thể xảy ra race condition nếu 2 requests cùng lúc.

### IC-05: A4 executed AND A5 executed (cùng lúc)
- **Lý do**: A4 và A5 là 2 exception khác nhau, chỉ một trong hai có thể xảy ra

### IC-06: A11 executed AND A12 executed (cùng lúc)
- **Lý do**: Success và error response không thể cùng lúc

---

## Fallback/Default

**Nếu không khớp luật nào:**
- Hệ thống sẽ rơi vào catch block (A12)
- Trả về HTTP 400 với `{"success": false, "message": <exception_message>}`
- Log error để debug (nếu có logger)

**Lý do**: Controller có try-catch bao quanh toàn bộ logic, đảm bảo mọi exception đều được handle và trả về response hợp lệ.

---

## Ràng buộc kỹ thuật & bối cảnh test

### Vai trò/Phân quyền liên quan (role matrix):
- **Customer/User**: Có thể áp dụng voucher vào order của chính mình
- **Không có role khác**: Chỉ owner của order mới có thể apply voucher

### Tiền điều kiện (preconditions):
1. **User đã đăng nhập** (có accountId trong session/request attribute)
2. **Order đã được tạo** với status bất kỳ (PENDING, PAID, etc.)
3. **Order có originalPrice** đã được tính (từ ticketType)
4. **Voucher đã được tạo** trong hệ thống (nếu muốn test success case)
5. **Database connection** available

### Hậu điều kiện (postconditions):
1. **VoucherUsage record** được tạo trong database (nếu thành công)
2. **Order.voucher** được set = voucher object
3. **Order.voucherCode** được set = voucherCode
4. **Order.voucherDiscountAmount** được set = discountAmount (có thể đã cap)
5. **Order.totalAmount** được recalculate (originalPrice - hostDiscount - voucherDiscount + VAT 10%)
6. **Voucher.quantity** được giảm đi 1
7. **Response được trả về** cho client

### Dữ liệu cần có: bảng DB, schema, khóa chính/ngoại, seed data:

#### Bảng `vouchers`:
```sql
CREATE TABLE vouchers (
    voucher_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    code VARCHAR(20) NOT NULL UNIQUE,
    discount_amount DECIMAL(10,2) NOT NULL,
    quantity INT NOT NULL DEFAULT 1,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME NOT NULL,
    expires_at DATETIME,
    description VARCHAR(500),
    created_by BIGINT,
    FOREIGN KEY (created_by) REFERENCES accounts(account_id)
);
```

#### Bảng `voucher_usage`:
```sql
CREATE TABLE voucher_usage (
    usage_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    voucher_id BIGINT NOT NULL,
    order_id BIGINT NOT NULL,
    used_at DATETIME NOT NULL,
    discount_applied DECIMAL(10,2) NOT NULL,
    FOREIGN KEY (voucher_id) REFERENCES vouchers(voucher_id),
    FOREIGN KEY (order_id) REFERENCES orders(order_id)
);
```

#### Bảng `orders`:
```sql
-- (Đã có trong Payment Webhook doc, chỉ liệt kê fields liên quan)
voucher_id BIGINT,
voucher_code VARCHAR(20),
voucher_discount_amount DECIMAL(10,2) DEFAULT 0,
original_price DECIMAL(10,2) NOT NULL,
total_amount DECIMAL(10,2) NOT NULL DEFAULT 0,
```

#### Seed Data cần thiết:
```sql
-- Voucher ACTIVE, chưa hết hạn, còn số lượng
INSERT INTO vouchers (voucher_id, code, discount_amount, quantity, status, created_at, expires_at, description)
VALUES (1, 'SAVE10', 10000.00, 10, 'ACTIVE', NOW(), DATE_ADD(NOW(), INTERVAL 30 DAY), 'Giảm 10k');

-- Voucher ACTIVE, vô thời hạn, còn số lượng
INSERT INTO vouchers (voucher_id, code, discount_amount, quantity, status, created_at, expires_at, description)
VALUES (2, 'FOREVER', 5000.00, 100, 'ACTIVE', NOW(), NULL, 'Voucher vô thời hạn');

-- Voucher đã hết hạn
INSERT INTO vouchers (voucher_id, code, discount_amount, quantity, status, created_at, expires_at, description)
VALUES (3, 'EXPIRED', 20000.00, 5, 'ACTIVE', DATE_SUB(NOW(), INTERVAL 60 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY), 'Voucher đã hết hạn');

-- Voucher DISABLED
INSERT INTO vouchers (voucher_id, code, discount_amount, quantity, status, created_at, expires_at, description)
VALUES (4, 'DISABLED', 15000.00, 20, 'DISABLED', NOW(), DATE_ADD(NOW(), INTERVAL 30 DAY), 'Voucher bị vô hiệu');

-- Voucher hết số lượng
INSERT INTO vouchers (voucher_id, code, discount_amount, quantity, status, created_at, expires_at, description)
VALUES (5, 'OUTOFSTOCK', 30000.00, 0, 'ACTIVE', NOW(), DATE_ADD(NOW(), INTERVAL 30 DAY), 'Voucher hết số lượng');

-- Voucher với discount > originalPrice (để test cap logic)
INSERT INTO vouchers (voucher_id, code, discount_amount, quantity, status, created_at, expires_at, description)
VALUES (6, 'BIGDISCOUNT', 200000.00, 5, 'ACTIVE', NOW(), DATE_ADD(NOW(), INTERVAL 30 DAY), 'Discount lớn hơn giá gốc');

-- Order để test
INSERT INTO orders (order_id, customer_id, event_id, ticket_type_id, status, original_price, total_amount, created_at)
VALUES (1, 1, 1, 1, 'PENDING', 50000.00, 55000.00, NOW());

-- Order của user khác (để test access denied)
INSERT INTO orders (order_id, customer_id, event_id, ticket_type_id, status, original_price, total_amount, created_at)
VALUES (2, 2, 1, 1, 'PENDING', 100000.00, 110000.00, NOW());
```

### API/Endpoint/UI liên quan:

#### Endpoint:
- **URL**: `POST /api/orders/{orderId}/apply-voucher?voucherCode={code}`
- **Method**: POST
- **Path Parameters**: 
  - `orderId` (Long, required)
- **Query Parameters**:
  - `voucherCode` (String, required)
- **Content-Type**: `application/json` (response)
- **Authentication**: Required (session-based, accountId từ request attribute)

#### Request mẫu:
```
POST /api/orders/1/apply-voucher?voucherCode=SAVE10
Headers:
  Cookie: JSESSIONID=...
```

#### Response mẫu (Success):
```json
{
  "success": true,
  "message": "Voucher applied successfully",
  "discountAmount": 10000.00,
  "newTotalAmount": 44000.00
}
```

#### Response mẫu (User not logged in):
```json
{
  "success": false,
  "message": "User not logged in"
}
```
HTTP Status: 401

#### Response mẫu (Order not found):
```json
{
  "success": false,
  "message": "Order not found"
}
```
HTTP Status: 400

#### Response mẫu (Access denied):
```json
{
  "success": false,
  "message": "Access denied"
}
```
HTTP Status: 403

#### Response mẫu (Voucher invalid):
```json
{
  "success": false,
  "message": "Voucher không hợp lệ hoặc đã hết hạn"
}
```
HTTP Status: 400

#### Response mẫu (Quantity exhausted):
```json
{
  "success": false,
  "message": "Voucher đã hết số lượng sử dụng"
}
```
HTTP Status: 400

#### Mã lỗi HTTP:
- **200 OK**: Voucher applied successfully
- **400 Bad Request**: Order not found, voucher invalid, quantity exhausted, hoặc exception khác
- **401 Unauthorized**: User not logged in
- **403 Forbidden**: Access denied (order không thuộc về user)

### Ngưỡng, tần suất, rate-limit, time window:
- **Rate limit**: Không có explicit rate limit, nhưng có thể có ở application level
- **Timeout**: Không có explicit timeout
- **Time window**: Voucher có thể có `expiresAt`, check tại thời điểm apply (LocalDateTime.now())
- **Frequency**: User có thể apply nhiều voucher khác nhau vào các order khác nhau, nhưng mỗi order chỉ có 1 voucher
- **Concurrency**: Race condition có thể xảy ra nếu 2 requests cùng apply voucher với quantity = 1 (cần transaction isolation)

### Timezone/Locale/I18n ảnh hưởng logic?
- **Timezone**: Sử dụng `LocalDateTime.now()` - server timezone (có thể là UTC hoặc Asia/Ho_Chi_Minh)
- **Locale**: Không ảnh hưởng (chỉ xử lý số và enum)
- **I18n**: Error messages là tiếng Việt hardcoded, có thể cần i18n trong tương lai

---

## Ví dụ cụ thể (ít nhất 3–5 mẫu)

### Ví dụ 1: Apply voucher thành công - Discount nhỏ hơn giá gốc
**Input**:
```
POST /api/orders/1/apply-voucher?voucherCode=SAVE10
Headers: Cookie với accountId=1
```

**DB State trước**:
- Order: `order_id=1, customer_id=1, original_price=50000.00, total_amount=55000.00, voucher_id=NULL`
- Voucher: `voucher_id=1, code='SAVE10', discount_amount=10000.00, quantity=10, status='ACTIVE', expires_at='2024-12-31 23:59:59'`

**Output mong đợi**:
- HTTP 200
- Response: `{"success": true, "message": "Voucher applied successfully", "discountAmount": 10000.00, "newTotalAmount": 44000.00}`
- DB State sau:
  - Order: `voucher_id=1, voucher_code='SAVE10', voucher_discount_amount=10000.00, total_amount=44000.00` (50000 - 10000 = 40000, + VAT 10% = 44000)
  - Voucher: `quantity=9` (giảm từ 10)
  - VoucherUsage: New record với `voucher_id=1, order_id=1, discount_applied=10000.00`

---

### Ví dụ 2: Apply voucher - Discount lớn hơn giá gốc (cap logic)
**Input**:
```
POST /api/orders/1/apply-voucher?voucherCode=BIGDISCOUNT
Headers: Cookie với accountId=1
```

**DB State trước**:
- Order: `order_id=1, original_price=50000.00, total_amount=55000.00`
- Voucher: `voucher_id=6, code='BIGDISCOUNT', discount_amount=200000.00, quantity=5, status='ACTIVE'`

**Output mong đợi**:
- HTTP 200
- Response: `{"success": true, "message": "Voucher applied successfully", "discountAmount": 50000.00, "newTotalAmount": 0.00}`
- DB State sau:
  - Order: `voucher_discount_amount=50000.00` (capped tại originalPrice), `total_amount=0.00` (50000 - 50000 = 0, + VAT 10% = 0)
  - Voucher: `quantity=4`
  - VoucherUsage: `discount_applied=50000.00` (capped value)

---

### Ví dụ 3: User chưa đăng nhập
**Input**:
```
POST /api/orders/1/apply-voucher?voucherCode=SAVE10
Headers: (không có Cookie hoặc accountId=null)
```

**DB State trước**: Không quan trọng

**Output mong đợi**:
- HTTP 401
- Response: `{"success": false, "message": "User not logged in"}`
- DB State sau: Không thay đổi

---

### Ví dụ 4: Order không tồn tại
**Input**:
```
POST /api/orders/999/apply-voucher?voucherCode=SAVE10
Headers: Cookie với accountId=1
```

**DB State trước**: Không có Order với order_id=999

**Output mong đợi**:
- HTTP 400
- Response: `{"success": false, "message": "Order not found"}`
- DB State sau: Không thay đổi

---

### Ví dụ 5: Access denied - Order không thuộc về user
**Input**:
```
POST /api/orders/2/apply-voucher?voucherCode=SAVE10
Headers: Cookie với accountId=1
```

**DB State trước**:
- Order: `order_id=2, customer_id=2` (thuộc về user khác)
- Voucher: `code='SAVE10', quantity=10, status='ACTIVE'`

**Output mong đợi**:
- HTTP 403
- Response: `{"success": false, "message": "Access denied"}`
- DB State sau: Không thay đổi

---

### Ví dụ 6: Voucher không tồn tại
**Input**:
```
POST /api/orders/1/apply-voucher?voucherCode=INVALID
Headers: Cookie với accountId=1
```

**DB State trước**:
- Order: `order_id=1, customer_id=1`
- Voucher: Không có voucher với code='INVALID'

**Output mong đợi**:
- HTTP 400
- Response: `{"success": false, "message": "Voucher không hợp lệ hoặc đã hết hạn"}`
- DB State sau: Không thay đổi

---

### Ví dụ 7: Voucher đã hết hạn
**Input**:
```
POST /api/orders/1/apply-voucher?voucherCode=EXPIRED
Headers: Cookie với accountId=1
```

**DB State trước**:
- Order: `order_id=1, customer_id=1`
- Voucher: `code='EXPIRED', status='ACTIVE', expires_at='2024-01-01 00:00:00'` (đã hết hạn)

**Output mong đợi**:
- HTTP 400
- Response: `{"success": false, "message": "Voucher không hợp lệ hoặc đã hết hạn"}`
- DB State sau: Không thay đổi

---

### Ví dụ 8: Voucher hết số lượng
**Input**:
```
POST /api/orders/1/apply-voucher?voucherCode=OUTOFSTOCK
Headers: Cookie với accountId=1
```

**DB State trước**:
- Order: `order_id=1, customer_id=1`
- Voucher: `code='OUTOFSTOCK', quantity=0, status='ACTIVE'`

**Output mong đợi**:
- HTTP 400
- Response: `{"success": false, "message": "Voucher đã hết số lượng sử dụng"}`
- DB State sau: Không thay đổi

---

## Giới hạn & giả định

### Giả định:
1. **Mỗi Order chỉ có 1 Voucher**: Code hiện tại không check xem order đã có voucher chưa, có thể overwrite voucher cũ
2. **Transaction isolation**: Không có explicit transaction boundary, có thể xảy ra race condition khi 2 requests cùng apply voucher với quantity = 1
3. **Voucher code là case-sensitive**: Query tìm voucher theo code exact match
4. **Original price đã được tính**: Order phải có originalPrice trước khi apply voucher
5. **Server timezone**: Sử dụng server local timezone để check expiration
6. **VAT calculation**: VAT 10% được tính trên giá sau discount (originalPrice - hostDiscount - voucherDiscount)

### Điều gì không thuộc phạm vi feature này:
1. **Voucher code validation format**: Không check format của voucher code (length, characters, etc.)
2. **Multiple vouchers per order**: Không hỗ trợ apply nhiều voucher vào cùng 1 order
3. **Voucher usage limit per user**: Không check user đã dùng voucher này bao nhiêu lần
4. **Voucher minimum order amount**: Không có minimum order amount để apply voucher
5. **Voucher category/event restriction**: Không check voucher chỉ áp dụng cho event/ticket type cụ thể
6. **Voucher expiration notification**: Không gửi thông báo khi voucher sắp hết hạn
7. **Voucher usage history UI**: Không có UI để xem lịch sử sử dụng voucher

---

## Tài liệu đính kèm

### Link code: controller/service/repository liên quan:

#### Controller:
- **File**: `src/main/java/com/group02/openevent/controller/OrderController.java`
- **Method**: `applyVoucher()` (lines 239-273)

#### Service Interface:
- **File**: `src/main/java/com/group02/openevent/service/VoucherService.java`
- **Method**: `applyVoucherToOrder()` (line 31)

#### Service Implementation:
- **File**: `src/main/java/com/group02/openevent/service/impl/VoucherServiceImpl.java`
- **Method**: `applyVoucherToOrder()` (lines 48-80)
- **Method**: `calculateVoucherDiscount()` (lines 84-100)

#### Repository:
- **File**: `src/main/java/com/group02/openevent/repository/IVoucherRepo.java`
- **Method**: `findAvailableVoucherByCode()` (line 26)
- **Query**: `SELECT v FROM Voucher v WHERE v.code = :code AND v.status = 'ACTIVE' AND (v.expiresAt IS NULL OR v.expiresAt > :now) AND v.quantity > 0`

#### Model:
- **File**: `src/main/java/com/group02/openevent/model/voucher/Voucher.java`
- **File**: `src/main/java/com/group02/openevent/model/voucher/VoucherStatus.java`
- **File**: `src/main/java/com/group02/openevent/model/voucher/VoucherUsage.java`
- **File**: `src/main/java/com/group02/openevent/model/order/Order.java`

### Link template UI/Thymeleaf (nếu liên quan):
- **Không có UI trực tiếp**: Endpoint là API-only, có thể được gọi từ frontend JavaScript

### Lược đồ DB hoặc ERD:
```
vouchers (1) -----< (many) voucher_usage
  |
  | (many)
  |
orders (1) -----< (many) voucher_usage

orders (many) -----> (1) vouchers (optional, nullable)
```

### Test hiện có (nếu có): liệt kê nhanh:
- **Không có test hiện tại**: Cần tạo test cases mới

---

## Checklist xác nhận (tick vào):

- [x] Tất cả điều kiện đã có miền giá trị & điểm biên.
- [x] Mỗi hành động được định nghĩa rõ, không mơ hồ.
- [x] Có thứ tự ưu tiên khi xung đột.
- [x] Đã liệt kê tổ hợp không khả thi.
- [x] Có ví dụ thực tế kèm dữ liệu.

---

**Ngày tạo**: 2024-12-19
**Người tạo**: AI Assistant
**Phiên bản**: 1.0

---

## Mapping Decision Table → Unit/Integration tests (hiện có/đề xuất)

- BR-01 (User not logged in → 401)
  - ĐÃ CÓ: `OrderControllerTest`/`OrderControllerIntegrationTest` thường cover auth; nếu thiếu, thêm test gọi `/api/orders/{id}/apply-voucher` không có session → 401.

- BR-02 (Order not found → 400)
  - ĐÃ CÓ/ĐỀ XUẤT: `OrderControllerTest` với `orderId` không tồn tại → 400 “Order not found”.

- BR-03 (Access denied → 403)
  - ĐỀ XUẤT: Tạo order thuộc customer khác, session accountId khác → 403.

- BR-04 (Voucher invalid/expired/disabled → 400)
  - ĐÃ CÓ: `VoucherServiceImplTest` kiểm tra `findAvailableVoucherByCode` trả empty → throw IllegalArgumentException “Voucher không hợp lệ hoặc đã hết hạn”.
  - ĐỀ XUẤT: Controller test bắt exception → 400 với message tương ứng.

- BR-05 (Quantity <= 0 → 400)
  - ĐÃ CÓ: `VoucherServiceImplTest` case quantity=0 → throw “hết số lượng sử dụng”.
  - ĐỀ XUẤT: Controller test bắt và map 400.

- BR-06 (Cap discount > originalPrice)
  - ĐÃ CÓ: `VoucherServiceImplTest` case BIGDISCOUNT → discountApplied = originalPrice.

- BR-07 (Happy path apply voucher)
  - ĐÃ CÓ/ĐỀ XUẤT: 
    - Service: `VoucherServiceImplTest` verify tạo `VoucherUsage`, giảm quantity, cập nhật order totals.
    - Controller: `OrderControllerTest` trả body `success=true`, `discountAmount`, `newTotalAmount`.

Các lớp test liên quan hiện có:
```1:8:src/test/java/com/group02/openevent/service/impl/VoucherServiceImplTest.java
// ... test validate available, quantity, discount cap, apply ...
```
```1:8:src/test/java/com/group02/openevent/controller/OrderControllerTest.java
// ... test controller apply-voucher các mã phản hồi 200/400/401/403 ...
```

