package com.group02.openevent.service.impl;

import com.group02.openevent.dto.payment.PayOSWebhookData;
import com.group02.openevent.dto.payment.PaymentResult;
import com.group02.openevent.model.event.Event;
import com.group02.openevent.model.order.Order;
import com.group02.openevent.model.order.OrderStatus;
import com.group02.openevent.model.payment.Payment;
import com.group02.openevent.model.payment.PaymentStatus;
import com.group02.openevent.repository.IOrderRepo;
import com.group02.openevent.repository.IPaymentRepo;
import com.group02.openevent.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.payos.PayOS;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentServiceImpl Unit Tests")
class PaymentServiceImplTest {

    @InjectMocks
    private PaymentServiceImpl paymentService;

    @Mock
    private IPaymentRepo paymentRepo;
    @Mock
    private IOrderRepo orderRepo;
    @Mock
    private OrderService orderService;
    @Mock
    private PayOS payOS;

    private Order order;
    private Payment payment;
    private Event event;

    private static final Long ORDER_ID = 1L;
    private static final Long PAYMENT_ID = 100L;
    private static final Long EVENT_ID = 10L;
    private static final String PAYMENT_LINK_ID = "pay123";
    private static final Long PAYMENT_LINK_ID_LONG = 12345L;

    @BeforeEach
    void setUp() {
        event = new Event();
        event.setId(EVENT_ID);
        event.setTitle("Test Event");

        order = new Order();
        order.setOrderId(ORDER_ID);
        order.setStatus(OrderStatus.PENDING);
        order.setEvent(event);
        order.setTotalAmount(BigDecimal.valueOf(100000));

        payment = new Payment();
        payment.setPaymentId(PAYMENT_ID);
        payment.setOrder(order);
        payment.setStatus(PaymentStatus.PENDING);
        payment.setPaymentLinkId(PAYMENT_LINK_ID);
        payment.setAmount(BigDecimal.valueOf(100000));
    }

    @Nested
    @DisplayName("verifyWebhook Tests")
    class VerifyWebhookTests {
        @Test
        @DisplayName("TC-01: Verify webhook successfully")
        void verifyWebhook_Success() {
            // Arrange
            PayOSWebhookData webhookData = new PayOSWebhookData();
            PayOSWebhookData.Data data = new PayOSWebhookData.Data();
            data.setOrderCode(ORDER_ID);
            data.setAmount(100000);
            webhookData.setData(data);

            // Act
            boolean result = paymentService.verifyWebhook(webhookData);

            // Assert
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("TC-02: Verify webhook returns false when data is null")
        void verifyWebhook_NullData() {
            // Arrange
            PayOSWebhookData webhookData = new PayOSWebhookData();
            webhookData.setData(null);

            // Act
            boolean result = paymentService.verifyWebhook(webhookData);

            // Assert
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("TC-03: Verify webhook returns false when webhookData is null")
        void verifyWebhook_NullWebhookData() {
            // Act
            boolean result = paymentService.verifyWebhook(null);

            // Assert
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("handlePaymentWebhook Tests")
    class HandlePaymentWebhookTests {
        @Test
        @DisplayName("TC-04: Handle payment webhook successfully")
        void handlePaymentWebhook_Success() {
            // Arrange
            PayOSWebhookData webhookData = new PayOSWebhookData();
            PayOSWebhookData.Data data = new PayOSWebhookData.Data();
            data.setPaymentLinkId(PAYMENT_LINK_ID_LONG);
            data.setOrderCode(ORDER_ID);
            data.setAmount(100000);
            webhookData.setData(data);

            when(paymentRepo.findByPaymentLinkId(String.valueOf(PAYMENT_LINK_ID_LONG))).thenReturn(Optional.of(payment));
            when(paymentRepo.save(any(Payment.class))).thenReturn(payment);
            when(orderRepo.save(any(Order.class))).thenReturn(order);

            // Act
            PaymentResult result = paymentService.handlePaymentWebhook(webhookData);

            // Assert
            assertThat(result.isSuccess()).isTrue();
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PAID);
            assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
            verify(paymentRepo).save(payment);
            verify(orderRepo).save(order);
        }

        @Test
        @DisplayName("TC-05: Handle webhook fails when payment not found")
        void handlePaymentWebhook_PaymentNotFound() {
            // Arrange
            PayOSWebhookData webhookData = new PayOSWebhookData();
            PayOSWebhookData.Data data = new PayOSWebhookData.Data();
            data.setPaymentLinkId(999L);
            webhookData.setData(data);

            when(paymentRepo.findByPaymentLinkId("999")).thenReturn(Optional.empty());

            // Act
            PaymentResult result = paymentService.handlePaymentWebhook(webhookData);

            // Assert
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getMessage()).contains("Payment not found");
        }

        @Test
        @DisplayName("TC-06: Handle webhook fails when webhook is invalid")
        void handlePaymentWebhook_InvalidWebhook() {
            // Arrange
            PayOSWebhookData webhookData = new PayOSWebhookData();
            webhookData.setData(null);

            // Act
            PaymentResult result = paymentService.handlePaymentWebhook(webhookData);

            // Assert
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getMessage()).contains("Invalid webhook signature");
        }
    }

    @Nested
    @DisplayName("updatePaymentStatus Tests")
    class UpdatePaymentStatusTests {
        @Test
        @DisplayName("TC-07: Update payment status successfully")
        void updatePaymentStatus_Success() {
            // Arrange
            when(paymentRepo.save(any(Payment.class))).thenReturn(payment);

            // Act
            paymentService.updatePaymentStatus(payment, PaymentStatus.PAID, 123L);

            // Assert
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PAID);
            assertThat(payment.getPayosPaymentId()).isEqualTo(123L);
            verify(paymentRepo).save(payment);
        }

        @Test
        @DisplayName("TC-08: Update payment status without payosPaymentId")
        void updatePaymentStatus_WithoutPayosPaymentId() {
            // Arrange
            when(paymentRepo.save(any(Payment.class))).thenReturn(payment);

            // Act
            paymentService.updatePaymentStatus(payment, PaymentStatus.EXPIRED, null);

            // Assert
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.EXPIRED);
            verify(paymentRepo).save(payment);
        }
    }

    @Nested
    @DisplayName("cancelPayment Tests")
    class CancelPaymentTests {
        @Test
        @DisplayName("TC-09: Cancel pending payment successfully")
        void cancelPayment_Pending_Success() {
            // Arrange
            payment.setStatus(PaymentStatus.PENDING);
            when(paymentRepo.save(any(Payment.class))).thenReturn(payment);
            when(orderRepo.save(any(Order.class))).thenReturn(order);

            // Act
            boolean result = paymentService.cancelPayment(payment);

            // Assert
            assertThat(result).isTrue();
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.CANCELLED);
            assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
            verify(paymentRepo).save(payment);
            verify(orderRepo).save(order);
        }

        @Test
        @DisplayName("TC-10: Cancel payment fails when already PAID")
        void cancelPayment_AlreadyPaid() {
            // Arrange
            payment.setStatus(PaymentStatus.PAID);

            // Act
            boolean result = paymentService.cancelPayment(payment);

            // Assert
            assertThat(result).isFalse();
            verify(paymentRepo, never()).save(any());
        }
    }

    @Nested
    @DisplayName("getPaymentsByCustomerId Tests")
    class GetPaymentsByCustomerIdTests {
        @Test
        @DisplayName("TC-11: Get payments by customer ID successfully")
        void getPaymentsByCustomerId_Success() {
            // Arrange
            Long customerId = 1L;
            when(paymentRepo.findByOrder_Customer_CustomerId(customerId)).thenReturn(List.of(payment));

            // Act
            List<Payment> result = paymentService.getPaymentsByCustomerId(customerId);

            // Assert
            assertThat(result).hasSize(1);
            assertThat(result.get(0)).isEqualTo(payment);
            verify(paymentRepo).findByOrder_Customer_CustomerId(customerId);
        }
    }

    @Nested
    @DisplayName("getPaymentsByCustomerIdAndStatus Tests")
    class GetPaymentsByCustomerIdAndStatusTests {
        @Test
        @DisplayName("TC-12: Get payments by customer ID and status successfully")
        void getPaymentsByCustomerIdAndStatus_Success() {
            // Arrange
            Long customerId = 1L;
            when(paymentRepo.findByOrder_Customer_CustomerIdAndStatus(customerId, PaymentStatus.PAID))
                    .thenReturn(List.of(payment));

            // Act
            List<Payment> result = paymentService.getPaymentsByCustomerIdAndStatus(customerId, PaymentStatus.PAID);

            // Assert
            assertThat(result).hasSize(1);
            assertThat(result.get(0)).isEqualTo(payment);
            verify(paymentRepo).findByOrder_Customer_CustomerIdAndStatus(customerId, PaymentStatus.PAID);
        }
    }

    @Nested
    @DisplayName("updateExpiredPayments Tests")
    class UpdateExpiredPaymentsTests {
        @Test
        @DisplayName("TC-13: Update expired payments successfully")
        void updateExpiredPayments_Success() {
            // Arrange
            Payment expiredPayment = new Payment();
            expiredPayment.setPaymentId(200L);
            expiredPayment.setStatus(PaymentStatus.PENDING);
            expiredPayment.setExpiredAt(LocalDateTime.now().minusMinutes(20));
            expiredPayment.setOrder(order);

            when(paymentRepo.findByStatus(PaymentStatus.PENDING)).thenReturn(List.of(expiredPayment));
            when(paymentRepo.save(any(Payment.class))).thenReturn(expiredPayment);
            when(orderRepo.save(any(Order.class))).thenReturn(order);

            // Act
            paymentService.updateExpiredPayments();

            // Assert
            assertThat(expiredPayment.getStatus()).isEqualTo(PaymentStatus.EXPIRED);
            assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
            verify(paymentRepo).save(expiredPayment);
            verify(orderRepo).save(order);
        }

        @Test
        @DisplayName("TC-14: Update expired payments skips non-expired")
        void updateExpiredPayments_SkipsNonExpired() {
            // Arrange
            Payment nonExpiredPayment = new Payment();
            nonExpiredPayment.setPaymentId(300L);
            nonExpiredPayment.setStatus(PaymentStatus.PENDING);
            nonExpiredPayment.setExpiredAt(LocalDateTime.now().plusMinutes(10));

            when(paymentRepo.findByStatus(PaymentStatus.PENDING)).thenReturn(List.of(nonExpiredPayment));

            // Act
            paymentService.updateExpiredPayments();

            // Assert
            assertThat(nonExpiredPayment.getStatus()).isEqualTo(PaymentStatus.PENDING);
            verify(paymentRepo, never()).save(any());
        }

        @Test
        @DisplayName("TC-15: Update expired payments handles null expiredAt")
        void updateExpiredPayments_NullExpiredAt() {
            // Arrange
            Payment paymentWithoutExpiry = new Payment();
            paymentWithoutExpiry.setPaymentId(400L);
            paymentWithoutExpiry.setStatus(PaymentStatus.PENDING);
            paymentWithoutExpiry.setExpiredAt(null);

            when(paymentRepo.findByStatus(PaymentStatus.PENDING)).thenReturn(List.of(paymentWithoutExpiry));

            // Act
            paymentService.updateExpiredPayments();

            // Assert
            assertThat(paymentWithoutExpiry.getStatus()).isEqualTo(PaymentStatus.PENDING);
            verify(paymentRepo, never()).save(any());
        }
    }

    @Nested
    @DisplayName("getPaymentByOrderId Tests")
    class GetPaymentByOrderIdTests {
        @Test
        @DisplayName("TC-16: Get payment by order ID successfully")
        void getPaymentByOrderId_Success() {
            // Arrange
            when(orderRepo.findById(ORDER_ID)).thenReturn(Optional.of(order));
            when(paymentRepo.findByOrder(order)).thenReturn(Optional.of(payment));

            // Act
            Optional<Payment> result = paymentService.getPaymentByOrderId(ORDER_ID);

            // Assert
            assertThat(result).isPresent();
            assertThat(result.get()).isEqualTo(payment);
            verify(orderRepo).findById(ORDER_ID);
            verify(paymentRepo).findByOrder(order);
        }

        @Test
        @DisplayName("TC-17: Get payment by order ID returns empty when order not found")
        void getPaymentByOrderId_OrderNotFound() {
            // Arrange
            when(orderRepo.findById(ORDER_ID)).thenReturn(Optional.empty());

            // Act
            Optional<Payment> result = paymentService.getPaymentByOrderId(ORDER_ID);

            // Assert
            assertThat(result).isEmpty();
            verify(orderRepo).findById(ORDER_ID);
            verify(paymentRepo, never()).findByOrder(any());
        }

        @Test
        @DisplayName("TC-18: Get payment by order ID returns empty when payment not found")
        void getPaymentByOrderId_PaymentNotFound() {
            // Arrange
            when(orderRepo.findById(ORDER_ID)).thenReturn(Optional.of(order));
            when(paymentRepo.findByOrder(order)).thenReturn(Optional.empty());

            // Act
            Optional<Payment> result = paymentService.getPaymentByOrderId(ORDER_ID);

            // Assert
            assertThat(result).isEmpty();
            verify(orderRepo).findById(ORDER_ID);
            verify(paymentRepo).findByOrder(order);
        }
    }

    @Nested
    @DisplayName("getPaymentByOrder Tests")
    class GetPaymentByOrderTests {
        @Test
        @DisplayName("TC-19: Get payment by order successfully")
        void getPaymentByOrder_Success() {
            // Arrange
            when(paymentRepo.findByOrder(order)).thenReturn(Optional.of(payment));

            // Act
            Optional<Payment> result = paymentService.getPaymentByOrder(order);

            // Assert
            assertThat(result).isPresent();
            assertThat(result.get()).isEqualTo(payment);
            verify(paymentRepo).findByOrder(order);
        }

        @Test
        @DisplayName("TC-20: Get payment by order returns empty when not found")
        void getPaymentByOrder_NotFound() {
            // Arrange
            when(paymentRepo.findByOrder(order)).thenReturn(Optional.empty());

            // Act
            Optional<Payment> result = paymentService.getPaymentByOrder(order);

            // Assert
            assertThat(result).isEmpty();
            verify(paymentRepo).findByOrder(order);
        }
    }

    @Nested
    @DisplayName("handlePaymentWebhookFromPayOS Tests")
    class HandlePaymentWebhookFromPayOSTests {
        @Test
        @DisplayName("TC-21: Handle payment webhook from PayOS returns failure when no data")
        void handlePaymentWebhookFromPayOS_NoData_ReturnsFailure() {
            // Arrange - webhook with no data
            PayOSWebhookData webhookData = new PayOSWebhookData();
            webhookData.setCode(0);
            webhookData.setDesc("Payment successful");
            webhookData.setData(null);

            // Act
            PaymentResult result = paymentService.handlePaymentWebhookFromPayOS(webhookData);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getMessage()).contains("Invalid webhook signature");
        }

        @Test
        @DisplayName("TC-22: Handle payment webhook from PayOS returns failure when payment link ID missing")
        void handlePaymentWebhookFromPayOS_MissingPaymentLinkId_ReturnsFailure() {
            // Arrange - webhook with data but no payment link ID
            PayOSWebhookData webhookData = new PayOSWebhookData();
            webhookData.setCode(0);
            webhookData.setDesc("Payment successful");

            PayOSWebhookData.Data data = new PayOSWebhookData.Data();
            data.setPaymentLinkId(null);
            data.setOrderCode(123L);
            webhookData.setData(data);

            // Act
            PaymentResult result = paymentService.handlePaymentWebhookFromPayOS(webhookData);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getMessage()).contains("Missing payment link ID");
        }
    }
}

