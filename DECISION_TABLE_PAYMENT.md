## Decision Table Testing: Nghiệp vụ Thanh toán (Payment Workflow)

Phạm vi: Dựa vào các test trong package `test` liên quan đến thanh toán (PayOS webhook, cập nhật trạng thái, hủy thanh toán, hết hạn, truy vấn bởi order/customer).
Nguồn chính: `PaymentServiceImplTest`. Tài liệu bổ trợ: `DECISION_TABLE_PAYMENT_WEBHOOK.md` (webhook chi tiết hơn).

### Bảng 1: Xác minh webhook (verifyWebhook)

Điều kiện:
- C1: `webhookData` có tồn tại?
- C2: `webhookData.data` có tồn tại?

Kết quả:
- R1: Trả về boolean hợp lệ

| Rule | C1 webhookData | C2 data | R1 isValid |
|---|---|---|---|
| V1 | Y | Y | true |
| V2 | Y | N | false |
| V3 | N | - | false |

Mapping test:
```82:96:src/test/java/com/group02/openevent/service/impl/PaymentServiceImplTest.java
@DisplayName("TC-01: Verify webhook successfully")
void verifyWebhook_Success() { ... assertThat(result).isTrue(); }
```
```98:110:src/test/java/com/group02/openevent/service/impl/PaymentServiceImplTest.java
@DisplayName("TC-02: Verify webhook returns false when data is null")
void verifyWebhook_NullData() { ... assertThat(result).isFalse(); }
```
```112:120:src/test/java/com/group02/openevent/service/impl/PaymentServiceImplTest.java
@DisplayName("TC-03: Verify webhook returns false when webhookData is null")
void verifyWebhook_NullWebhookData() { ... assertThat(result).isFalse(); }
```


### Bảng 2: Xử lý webhook (handlePaymentWebhook)

Điều kiện:
- C1: `webhookData.data` tồn tại?
- C2: `paymentLinkId` có tồn tại trong `data`?
- C3: Tìm thấy `Payment` theo `paymentLinkId`?

Kết quả:
- R1: `PaymentResult.success`
- R2: Cập nhật `Payment.status = PAID`
- R3: Cập nhật `Order.status = PAID`

| Rule | C1 data | C2 paymentLinkId | C3 paymentFound | R1 success | R2 Payment=PAID | R3 Order=PAID |
|---|---|---|---|---|---|---|
| H1 (Happy) | Y | Y | Y | true | Y | Y |
| H2 (Không tìm thấy Payment) | Y | Y | N | false | N | N |
| H3 (Webhook invalid) | N | - | - | false | N | N |

Mapping test:
```126:150:src/test/java/com/group02/openevent/service/impl/PaymentServiceImplTest.java
@DisplayName("TC-04: Handle payment webhook successfully")
void handlePaymentWebhook_Success() { ... assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PAID); }
```
```152:169:src/test/java/com/group02/openevent/service/impl/PaymentServiceImplTest.java
@DisplayName("TC-05: Handle webhook fails when payment not found")
void handlePaymentWebhook_PaymentNotFound() { ... assertThat(result.isSuccess()).isFalse(); }
```
```171:184:src/test/java/com/group02/openevent/service/impl/PaymentServiceImplTest.java
@DisplayName("TC-06: Handle webhook fails when webhook is invalid")
void handlePaymentWebhook_InvalidWebhook() { ... assertThat(result.isSuccess()).isFalse(); }
```


### Bảng 3: Xử lý webhook PayOS nâng cao (handlePaymentWebhookFromPayOS)

Điều kiện:
- C1: `webhookData.data` tồn tại?
- C2: `paymentLinkId` trong `data` tồn tại?

Kết quả:
- R1: `PaymentResult.success`
- R2: Thông báo lỗi tương ứng

| Rule | C1 data | C2 paymentLinkId | R1 success | R2 message |
|---|---|---|---|---|
| P1 | N | - | false | "Invalid webhook signature" |
| P2 | Y | N | false | "Missing payment link ID" |

Mapping test:
```451:468:src/test/java/com/group02/openevent/service/impl/PaymentServiceImplTest.java
@DisplayName("TC-21: ... no data")
void handlePaymentWebhookFromPayOS_NoData_ReturnsFailure() { ... }
```
```470:489:src/test/java/com/group02/openevent/service/impl/PaymentServiceImplTest.java
@DisplayName("TC-22: ... missing payment link ID")
void handlePaymentWebhookFromPayOS_MissingPaymentLinkId_ReturnsFailure() { ... }
```


### Bảng 4: Cập nhật trạng thái thanh toán (updatePaymentStatus)

Điều kiện:
- C1: Trạng thái mới `newStatus` hợp lệ?
- C2: `payosPaymentId` có được cung cấp?

Kết quả:
- R1: Lưu `Payment.status = newStatus`
- R2: Nếu có, lưu `payosPaymentId`

| Rule | C1 newStatus | C2 payosPaymentId | R1 set status | R2 set payosPaymentId |
|---|---|---|---|---|
| U1 | PAID | Y | Y | Y |
| U2 | EXPIRED | N | Y | N |

Mapping test:
```190:203:src/test/java/com/group02/openevent/service/impl/PaymentServiceImplTest.java
@DisplayName("TC-07: Update payment status successfully")
void updatePaymentStatus_Success() { ... }
```
```205:217:src/test/java/com/group02/openevent/service/impl/PaymentServiceImplTest.java
@DisplayName("TC-08: Update payment status without payosPaymentId")
void updatePaymentStatus_WithoutPayosPaymentId() { ... }
```


### Bảng 5: Hủy thanh toán (cancelPayment)

Điều kiện:
- C1: `Payment.status = PENDING`?

Kết quả:
- R1: `return true/false`
- R2: Nếu true, `Payment.status = CANCELLED`
- R3: Nếu true, `Order.status = CANCELLED`

| Rule | C1 isPending | R1 success | R2 Payment=CANCELLED | R3 Order=CANCELLED |
|---|---|---|---|---|
| C1 (Hủy thành công) | Y | true | Y | Y |
| C2 (Đã PAID, không hủy) | N | false | N | N |

Mapping test:
```223:240:src/test/java/com/group02/openevent/service/impl/PaymentServiceImplTest.java
@DisplayName("TC-09: Cancel pending payment successfully")
void cancelPayment_Pending_Success() { ... }
```
```242:254:src/test/java/com/group02/openevent/service/impl/PaymentServiceImplTest.java
@DisplayName("TC-10: Cancel payment fails when already PAID")
void cancelPayment_AlreadyPaid() { ... }
```


### Bảng 6: Tự động hết hạn thanh toán (updateExpiredPayments)

Điều kiện (cho từng `Payment` trạng thái PENDING được quét):
- C1: `expiredAt` có null?
- C2: `expiredAt` đã trước `now`?

Kết quả:
- R1: Nếu C2, đặt `Payment.status = EXPIRED` và `Order.status = CANCELLED`
- R2: Ngược lại, giữ nguyên

| Rule | C1 expiredAt null | C2 expiredAt < now | R1 expire+cancel order | R2 keep |
|---|---|---|---|---|
| E1 (Hết hạn) | N | Y | Y | N |
| E2 (Chưa hết hạn) | N | N | N | Y |
| E3 (Không có expiredAt) | Y | - | N | Y |

Mapping test:
```301:323:src/test/java/com/group02/openevent/service/impl/PaymentServiceImplTest.java
@DisplayName("TC-13: Update expired payments successfully")
void updateExpiredPayments_Success() { ... }
```
```325:342:src/test/java/com/group02/openevent/service/impl/PaymentServiceImplTest.java
@DisplayName("TC-14: Update expired payments skips non-expired")
void updateExpiredPayments_SkipsNonExpired() { ... }
```
```344:361:src/test/java/com/group02/openevent/service/impl/PaymentServiceImplTest.java
@DisplayName("TC-15: Update expired payments handles null expiredAt")
void updateExpiredPayments_NullExpiredAt() { ... }
```


### Bảng 7: Truy vấn Payment theo Order

Điều kiện:
- C1: Tồn tại `Order` theo `orderId`?
- C2: Tồn tại `Payment` theo `Order`?

Kết quả:
- R1: Optional<Payment> có giá trị?

| Rule | C1 orderFound | C2 paymentFound | R1 result |
|---|---|---|---|
| Q1 | Y | Y | Present |
| Q2 | Y | N | Empty |
| Q3 | N | - | Empty |

Mapping test:
```368:382:src/test/java/com/group02/openevent/service/impl/PaymentServiceImplTest.java
@DisplayName("TC-16: Get payment by order ID successfully")
void getPaymentByOrderId_Success() { ... }
```
```384:397:src/test/java/com/group02/openevent/service/impl/PaymentServiceImplTest.java
@DisplayName("TC-17: ... order not found") void getPaymentByOrderId_OrderNotFound() { ... }
```
```399:413:src/test/java/com/group02/openevent/service/impl/PaymentServiceImplTest.java
@DisplayName("TC-18: ... payment not found") void getPaymentByOrderId_PaymentNotFound() { ... }
```


### Bảng 8: Truy vấn Payment của khách hàng

Điều kiện:
- C1: Có `customerId` hợp lệ
- C2: Lọc theo `PaymentStatus` hay không

Kết quả:
- R1: Trả danh sách Payment

| Rule | C1 customerId | C2 status filter | R1 result |
|---|---|---|---|
| CUS1 | Y | N | List<Payment> by customer |
| CUS2 | Y | Y | List<Payment> by customer and status |

Mapping test:
```260:274:src/test/java/com/group02/openevent/service/impl/PaymentServiceImplTest.java
@DisplayName("TC-11: Get payments by customer ID successfully")
void getPaymentsByCustomerId_Success() { ... }
```
```279:295:src/test/java/com/group02/openevent/service/impl/PaymentServiceImplTest.java
@DisplayName("TC-12: Get payments by customer ID and status successfully")
void getPaymentsByCustomerIdAndStatus_Success() { ... }
```


### Ghi chú thiết kế test theo bảng quyết định
- Webhook PayOS: kiểm tra sự tồn tại của `data` và `paymentLinkId` trước khi xử lý; nếu hợp lệ thì cập nhật trạng thái Payment/Order.
- Hủy thanh toán chỉ được phép khi Payment còn `PENDING`.
- Tự động hết hạn chỉ áp dụng cho các Payment `PENDING` có `expiredAt` trước thời điểm hiện tại.
- Các truy vấn đảm bảo null-safety: không tìm thấy Order/Payment trả về rỗng.















