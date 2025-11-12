
---

## Mapping Decision Table → Unit/Integration tests (hiện có/đề xuất)

- BR-01/BR-02 (Empty webhook body / No data → 200 ok, data=null)
  - ĐỀ XUẤT: Thêm test ở `PaymentControllerTest`: POST `/api/payments/webhook` với `{}` và với `null` body → expect 200, `error=0`, `data=null`.

- BR-03 (Tìm theo orderId từ description; fallback orderCode)
  - ĐỀ XUẤT: Mock service để khi description chứa `"Order 1"` thì tìm thấy Payment; khi không có, dùng orderCode để tìm Order→Payment. Viết 2 test tách biệt.

- BR-04 (Payment not found → success=false, message “Payment not found”)
  - ĐÃ CÓ: `PaymentControllerTest`/`PaymentControllerIntegrationTest` có thể đã cover “not found”; nếu chưa, thêm case trả `success=false`.

- BR-05 (Payment đã PAID → skip update)
  - ĐỀ XUẤT: `PaymentServiceImplTest`: payment.status=PAID trước khi handle → sau handle không thay đổi order/payment; verify save không bị gọi set lại.

- BR-06 (Cập nhật Payment & Order sang PAID)
  - ĐÃ CÓ: `PaymentServiceImplTest` xử lý webhook thành công → set `payment=PAID`, `order=PAID`.
  - ĐỀ XUẤT: Integration: POST webhook với `data.paymentLinkId` trùng DB → assert DB cập nhật.

- BR-07 (Exception → vẫn trả 200, data.success=false, error=<msg>)
  - ĐỀ XUẤT: `PaymentControllerTest`: mock service ném RuntimeException → assert HTTP 200, body `{error:0, data:{success:false}}`.

Vị trí test hiện có liên quan:
```1:12:src/test/java/com/group02/openevent/controller/PaymentControllerTest.java
// ... các test controller payment ...
```
```1:12:src/test/java/com/group02/openevent/service/impl/PaymentServiceImplTest.java
// ... các test service webhook/payment update ...
```

