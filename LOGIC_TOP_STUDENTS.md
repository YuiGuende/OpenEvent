# Logic Lấy Top Students - Đơn Giản

## Vấn đề ban đầu
- Lỗi `UnexpectedRollbackException`: Transaction bị rollback
- Nguyên nhân: Query với `JOIN FETCH` cố load Account mà Account không tồn tại → Exception → Transaction rollback

## Logic mới (Đơn giản)

### 1. Repository (`ICustomerRepo.java`)
```java
@Query(value = "SELECT customer_id, name, points, email, image_url " +
        "FROM customer " +
        "WHERE name IS NOT NULL AND name != '' AND name != 'Chưa có dữ liệu' " +
        "ORDER BY points DESC " +
        "LIMIT 3", nativeQuery = true)
List<Object[]> findTopStudentsByPointsNative();
```

**Giải thích:**
- Sử dụng **Native SQL query** (không phải JPQL)
- Query trực tiếp từ bảng `customer` trong database
- Lọc: `name IS NOT NULL AND name != '' AND name != 'Chưa có dữ liệu'`
- Sắp xếp: `ORDER BY points DESC` (điểm cao nhất trước)
- Giới hạn: `LIMIT 3` (lấy top 3)
- Trả về: `List<Object[]>` - mảng các giá trị: `[customer_id, name, points, email, image_url]`
- **KHÔNG load Entity** → Tránh lazy load và transaction rollback

### 2. Service (`TopStudentServiceImpl.java`)

**Bước 1: Query từ database bằng native SQL**
```java
List<Object[]> results = customerRepo.findTopStudentsByPointsNative();
```

**Bước 2: Parse kết quả và tạo DTO**
```java
for (Object[] row : results) {
    // Parse từ Object[]: [customer_id, name, points, email, image_url]
    Long customerId = ((Number) row[0]).longValue();
    String name = row[1].toString();
    Integer points = ((Number) row[2]).intValue();
    String email = row[3] != null ? row[3].toString() : "";
    String imageUrl = row[4] != null ? row[4].toString() : null;
    
    // Tạo DTO
    TopStudentDTO dto = TopStudentDTO.builder()
        .customerId(customerId)
        .name(name)
        .points(points)
        .email(email)
        .imageUrl(imageUrl != null ? imageUrl : getDefaultImageUrl(customerId))
        .rank(rank++)
        .build();
    
    topStudents.add(dto);
}
```

**Bước 3: Đảm bảo có đủ 3 students**
```java
while (topStudents.size() < 3) {
    topStudents.add(createPlaceholderStudent(...)); // Thêm placeholder nếu thiếu
}
```

### 3. Controller (`HomeController.java`)

**Bước 1: Gọi service**
```java
List<TopStudentDTO> topStudents = topStudentService.getTopStudents(3);
```

**Bước 2: Đảm bảo có dữ liệu**
```java
if (topStudents.isEmpty()) {
    // Tạo placeholder
}
```

**Bước 3: Add vào model**
```java
model.addAttribute("topStudents", topStudents);
```

### 4. Template (`index.html`)

**Hiển thị 3 students:**
```html
<!-- Rank 1 -->
<div th:if="${topStudents != null and !topStudents.isEmpty()}">
    <div th:text="${topStudents[0]?.name}">Name</div>
    <div th:text="${topStudents[0]?.points + ' Points'}">Points</div>
</div>

<!-- Rank 2 -->
<div th:if="${topStudents.size() >= 2}">
    <div th:text="${topStudents[1]?.name}">Name</div>
</div>

<!-- Rank 3 -->
<div th:if="${topStudents.size() >= 3}">
    <div th:text="${topStudents[2]?.name}">Name</div>
</div>
```

## Luồng dữ liệu

```
Database (customer table)
    ↓
Repository.findTopStudentsByPointsNative()
    ↓ (Native SQL: SELECT customer_id, name, points, email, image_url 
       FROM customer WHERE name IS NOT NULL ORDER BY points DESC LIMIT 3)
List<Object[]> (Raw data: [id, name, points, email, image_url])
    ↓
Service.getTopStudents(3)
    ↓ (Parse Object[], tạo DTO, thêm placeholder nếu thiếu)
List<TopStudentDTO>
    ↓
Controller.home()
    ↓ (Add vào model)
Model.addAttribute("topStudents", ...)
    ↓
Template (index.html)
    ↓ (Thymeleaf render)
HTML hiển thị 3 students
```

## Tại sao đơn giản hơn?

### Trước (Phức tạp - Gây lỗi):
1. ❌ Query với `JOIN FETCH c.account` → Cố load Account
2. ❌ Nếu Account không tồn tại → Exception
3. ❌ Exception → Transaction rollback
4. ❌ Kết quả: Không có dữ liệu

### Sau (Đơn giản - Hoạt động):
1. ✅ **Native SQL query** → Query trực tiếp từ database, không qua JPA Entity
2. ✅ Trả về `Object[]` → Chỉ lấy dữ liệu cần thiết, không load Entity
3. ✅ Không có lazy load → Không có exception
4. ✅ Không có transaction rollback → Transaction thành công
5. ✅ Kết quả: Có dữ liệu hiển thị

## Các trường dữ liệu cần thiết

### Trong Database (`customer` table):
- ✅ `customer_id` (PK)
- ✅ `name` (VARCHAR) - **BẮT BUỘC**: Không NULL, không rỗng
- ✅ `points` (INT) - **BẮT BUỘC**: Để sắp xếp
- ✅ `email` (VARCHAR) - Optional: Để hiển thị
- ✅ `image_url` (VARCHAR) - Optional: Để hiển thị ảnh

### KHÔNG cần:
- ❌ `account_id` - Không cần kiểm tra Account tồn tại
- ❌ `organization_id` - Không cần load Organization

## Kiểm tra nhanh

### 1. Kiểm tra database có dữ liệu:
```sql
SELECT customer_id, name, points, email 
FROM customer 
WHERE name IS NOT NULL AND name != '' AND name != 'Chưa có dữ liệu'
ORDER BY points DESC 
LIMIT 3;
```

### 2. Kiểm tra service trả về gì:
- Truy cập: `http://localhost:8080/api/debug/top-students`
- Xem JSON response

### 3. Kiểm tra logs:
- Tìm: `DEBUG: Query findTopStudentsByPoints() returned X customers`
- Tìm: `DEBUG: ✓ Added student - Name: '...', Points: ...`

## Tóm tắt

**Logic đơn giản:**
1. **Native SQL query** → Query trực tiếp từ database: `SELECT customer_id, name, points, email, image_url FROM customer WHERE ... ORDER BY points DESC LIMIT 3`
2. Parse kết quả từ `Object[]` → Lấy từng giá trị: `customer_id`, `name`, `points`, `email`, `image_url`
3. Tạo DTO từ dữ liệu raw → Không cần load Entity
4. Thêm placeholder nếu thiếu → Đảm bảo luôn có 3 students
5. Trả về cho Controller → `List<TopStudentDTO>`
6. Controller add vào model → `model.addAttribute("topStudents", ...)`
7. Template hiển thị → Thymeleaf render HTML

**Không làm gì:**
- ❌ Không load JPA Entity (`Customer`)
- ❌ Không JOIN với Account
- ❌ Không truy cập `customer.getAccount()`
- ❌ Không load Organization
- ❌ Không dùng JOIN FETCH
- ❌ Không dùng JPQL (Java Persistence Query Language)

**Chỉ làm:**
- ✅ **Native SQL query** → Query trực tiếp từ database
- ✅ Parse `Object[]` → Lấy dữ liệu raw
- ✅ Tạo DTO từ dữ liệu raw → Không cần Entity
- ✅ Sắp xếp theo points (trong SQL)
- ✅ Lấy top 3 (LIMIT 3 trong SQL)

