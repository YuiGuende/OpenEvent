# Payment – Decision Table Coverage

This report maps Decision Table rules (from docs/testing/Decision_Table_Payment.docx) to existing tests, marks coverage, and provides evidence.

Legend:
- Covered?: Yes / Partial / No
- Evidence: test class#method + key assertion/verify and code reference

| Table | Rule | Covered? | Evidence (test class#method, line/assert) | Notes |
|------|------|-----------|--------------------------------------------|-------|
| verifyWebhook | V1 | Yes | PaymentServiceImplTest#verifyWebhook_Success – assertTrue | See reference below |
| verifyWebhook | V2 | Yes | PaymentServiceImplTest#verifyWebhook_NullData – assertFalse |  |
| verifyWebhook | V3 | Yes | PaymentServiceImplTest#verifyWebhook_NullWebhookData – assertFalse |  |
| handlePaymentWebhook | H1 | Yes | PaymentServiceImplTest#handlePaymentWebhook_Success – success=true, Payment=PAID, Order=PAID |  |
| handlePaymentWebhook | H2 | Yes | PaymentServiceImplTest#handlePaymentWebhook_PaymentNotFound – success=false |  |
| handlePaymentWebhook | H3 | Yes | PaymentServiceImplTest#handlePaymentWebhook_InvalidWebhook – success=false |  |
| handlePaymentWebhookFromPayOS | P1 | Yes | PaymentServiceImplTest#handlePaymentWebhookFromPayOS_NoData_ReturnsFailure – message contains “Invalid webhook signature” |  |
| handlePaymentWebhookFromPayOS | P2 | Yes | PaymentServiceImplTest#handlePaymentWebhookFromPayOS_MissingPaymentLinkId_ReturnsFailure – message contains “Missing payment link ID” |  |
| updatePaymentStatus | U1 | Yes | PaymentServiceImplTest#updatePaymentStatus_Success – status=PAID, id set |  |
| updatePaymentStatus | U2 | Yes | PaymentServiceImplTest#updatePaymentStatus_WithoutPayosPaymentId – status=EXPIRED, id not set |  |
| cancelPayment | C1 | Yes | PaymentServiceImplTest#cancelPayment_Pending_Success – success=true, Payment=CANCELLED, Order=CANCELLED |  |
| cancelPayment | C2 | Yes | PaymentServiceImplTest#cancelPayment_AlreadyPaid – success=false |  |
| updateExpiredPayments | E1 | Yes | PaymentServiceImplTest#updateExpiredPayments_Success – Payment=EXPIRED, Order=CANCELLED |  |
| updateExpiredPayments | E2 | Yes | PaymentServiceImplTest#updateExpiredPayments_SkipsNonExpired – stays PENDING |  |
| updateExpiredPayments | E3 | Yes | PaymentServiceImplTest#updateExpiredPayments_NullExpiredAt – stays PENDING |  |
| query payment by order | Q1 | Yes | PaymentServiceImplTest#getPaymentByOrderId_Success – Optional present |  |
| query payment by order | Q2 | Yes | PaymentServiceImplTest#getPaymentByOrderId_PaymentNotFound – Optional empty |  |
| query payment by order | Q3 | Yes | PaymentServiceImplTest#getPaymentByOrderId_OrderNotFound – Optional empty |  |
| query payments of customer | CUS1 | Yes | PaymentServiceImplTest#getPaymentsByCustomerId_Success – list size=1 |  |
| query payments of customer | CUS2 | Yes | PaymentServiceImplTest#getPaymentsByCustomerIdAndStatus_Success – filtered list size=1 |  |

### Code references (evidence)

```82:96:src/test/java/com/group02/openevent/service/impl/PaymentServiceImplTest.java
@Test
@DisplayName("TC-01: Verify webhook successfully")
void verifyWebhook_Success() {
    // ...
    assertThat(result).isTrue();
}
```

```98:110:src/test/java/com/group02/openevent/service/impl/PaymentServiceImplTest.java
@Test
@DisplayName("TC-02: Verify webhook returns false when data is null")
void verifyWebhook_NullData() {
    // ...
    assertThat(result).isFalse();
}
```

```112:120:src/test/java/com/group02/openevent/service/impl/PaymentServiceImplTest.java
@Test
@DisplayName("TC-03: Verify webhook returns false when webhookData is null")
void verifyWebhook_NullWebhookData() {
    // ...
    assertThat(result).isFalse();
}
```

```126:150:src/test/java/com/group02/openevent/service/impl/PaymentServiceImplTest.java
@Test
@DisplayName("TC-04: Handle payment webhook successfully")
void handlePaymentWebhook_Success() {
    // ...
    assertThat(result.isSuccess()).isTrue();
    assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PAID);
    assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
}
```

```152:169:src/test/java/com/group02/openevent/service/impl/PaymentServiceImplTest.java
@Test
@DisplayName("TC-05: Handle webhook fails when payment not found")
void handlePaymentWebhook_PaymentNotFound() {
    // ...
    assertThat(result.isSuccess()).isFalse();
    assertThat(result.getMessage()).contains("Payment not found");
}
```

```171:184:src/test/java/com/group02/openevent/service/impl/PaymentServiceImplTest.java
@Test
@DisplayName("TC-06: Handle webhook fails when webhook is invalid")
void handlePaymentWebhook_InvalidWebhook() {
    // ...
    assertThat(result.isSuccess()).isFalse();
    assertThat(result.getMessage()).contains("Invalid webhook signature");
}
```

```451:468:src/test/java/com/group02/openevent/service/impl/PaymentServiceImplTest.java
@Test
@DisplayName("TC-21: Handle payment webhook from PayOS returns failure when no data")
void handlePaymentWebhookFromPayOS_NoData_ReturnsFailure() {
    // ...
    assertThat(result.isSuccess()).isFalse();
    assertThat(result.getMessage()).contains("Invalid webhook signature");
}
```

```470:489:src/test/java/com/group02/openevent/service/impl/PaymentServiceImplTest.java
@Test
@DisplayName("TC-22: Handle payment webhook from PayOS returns failure when payment link ID missing")
void handlePaymentWebhookFromPayOS_MissingPaymentLinkId_ReturnsFailure() {
    // ...
    assertThat(result.isSuccess()).isFalse();
    assertThat(result.getMessage()).contains("Missing payment link ID");
}
```

```190:203:src/test/java/com/group02/openevent/service/impl/PaymentServiceImplTest.java
@Test
@DisplayName("TC-07: Update payment status successfully")
void updatePaymentStatus_Success() {
    // ...
    assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PAID);
    assertThat(payment.getPayosPaymentId()).isEqualTo(123L);
}
```

```205:217:src/test/java/com/group02/openevent/service/impl/PaymentServiceImplTest.java
@Test
@DisplayName("TC-08: Update payment status without payosPaymentId")
void updatePaymentStatus_WithoutPayosPaymentId() {
    // ...
    assertThat(payment.getStatus()).isEqualTo(PaymentStatus.EXPIRED);
}
```

```223:240:src/test/java/com/group02/openevent/service/impl/PaymentServiceImplTest.java
@Test
@DisplayName("TC-09: Cancel pending payment successfully")
void cancelPayment_Pending_Success() {
    // ...
    assertThat(result).isTrue();
    assertThat(payment.getStatus()).isEqualTo(PaymentStatus.CANCELLED);
    assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
}
```

```242:254:src/test/java/com/group02/openevent/service/impl/PaymentServiceImplTest.java
@Test
@DisplayName("TC-10: Cancel payment fails when already PAID")
void cancelPayment_AlreadyPaid() {
    // ...
    assertThat(result).isFalse();
}
```

```301:323:src/test/java/com/group02/openevent/service/impl/PaymentServiceImplTest.java
@Test
@DisplayName("TC-13: Update expired payments successfully")
void updateExpiredPayments_Success() {
    // ...
    assertThat(expiredPayment.getStatus()).isEqualTo(PaymentStatus.EXPIRED);
    assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
}
```

```325:342:src/test/java/com/group02/openevent/service/impl/PaymentServiceImplTest.java
@Test
@DisplayName("TC-14: Update expired payments skips non-expired")
void updateExpiredPayments_SkipsNonExpired() {
    // ...
    assertThat(nonExpiredPayment.getStatus()).isEqualTo(PaymentStatus.PENDING);
}
```

```344:361:src/test/java/com/group02/openevent/service/impl/PaymentServiceImplTest.java
@Test
@DisplayName("TC-15: Update expired payments handles null expiredAt")
void updateExpiredPayments_NullExpiredAt() {
    // ...
    assertThat(paymentWithoutExpiry.getStatus()).isEqualTo(PaymentStatus.PENDING);
}
```

```368:382:src/test/java/com/group02/openevent/service/impl/PaymentServiceImplTest.java
@Test
@DisplayName("TC-16: Get payment by order ID successfully")
void getPaymentByOrderId_Success() {
    // ...
    assertThat(result).isPresent();
}
```

```384:397:src/test/java/com/group02/openevent/service/impl/PaymentServiceImplTest.java
@Test
@DisplayName("TC-17: Get payment by order ID returns empty when order not found")
void getPaymentByOrderId_OrderNotFound() {
    // ...
    assertThat(result).isEmpty();
}
```

```399:413:src/test/java/com/group02/openevent/service/impl/PaymentServiceImplTest.java
@Test
@DisplayName("TC-18: Get payment by order ID returns empty when payment not found")
void getPaymentByOrderId_PaymentNotFound() {
    // ...
    assertThat(result).isEmpty();
}
```

```260:274:src/test/java/com/group02/openevent/service/impl/PaymentServiceImplTest.java
@Test
@DisplayName("TC-11: Get payments by customer ID successfully")
void getPaymentsByCustomerId_Success() {
    // ...
    assertThat(result).hasSize(1);
}
```

```279:295:src/test/java/com/group02/openevent/service/impl/PaymentServiceImplTest.java
@Test
@DisplayName("TC-12: Get payments by customer ID and status successfully")
void getPaymentsByCustomerIdAndStatus_Success() {
    // ...
    assertThat(result).hasSize(1);
}
```

## Summary
- Total rules: 20
- Covered: 20
- Partial: 0
- Missing: 0
- Coverage: 100%

⚠️ No critical gaps detected (H1/H2/H3, E1/E2/E3, C1/C2 all covered).

## Gợi ý test còn thiếu
Không có rule thiếu theo Decision Table đã cung cấp. Nếu mở rộng Decision Table (ví dụ: lỗi định dạng amount, very large amount, concurrency), đề xuất sẽ được bổ sung.

## Console summary
Covered 20/20 rules (100%). No missing or partial items.















