package com.group02.openevent.service.impl;

import com.group02.openevent.model.event.Event;
import com.group02.openevent.model.order.Order;
import com.group02.openevent.model.user.Customer;
import com.group02.openevent.model.voucher.Voucher;
import com.group02.openevent.model.voucher.VoucherStatus;
import com.group02.openevent.model.voucher.VoucherUsage;
import com.group02.openevent.repository.IVoucherRepo;
import com.group02.openevent.repository.IVoucherUsageRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * FILE TEST DỊCH VỤ ĐÃ GỘP
 * Test Framework: JUnit 5 with Mockito
 * Test Style: BDD (Given/When/Then) với @Nested
 * * Test Coverage:
 * 1. applyVoucherToOrder (Chi tiết từ File 2)
 * 2. calculateVoucherDiscount (Từ File 1)
 * 3. isVoucherAvailable (Từ File 1)
 * 4. getVoucherByCode (Từ File 1)
 * 5. getAvailableVouchers (Từ File 1)
 * 6. createVoucher (Từ File 1)
 * 7. disableVoucher (Từ File 1)
 * 8. updateVoucherQuantity (Từ File 1)
 * 9. getVoucherUsageHistory (Từ File 1)
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("VoucherServiceImpl BDD Tests")
class VoucherServiceImplTest {

    @Mock
    private IVoucherRepo voucherRepo;

    @Mock
    private IVoucherUsageRepo voucherUsageRepo;

    @InjectMocks
    private VoucherServiceImpl voucherService;

    // Biến dùng chung cho các test (NGOẠI TRỪ applyVoucherToOrder)
    private Voucher generalValidVoucher;
    private Order generalValidOrder;
    private String generalValidVoucherCode;

    @BeforeEach
    void setUpGeneral() {
        // Given: Common test data setup for general tests
        generalValidVoucherCode = "SAVE20";
        generalValidVoucher = createGeneralValidVoucher();
        generalValidOrder = createGeneralValidOrder();
    }

    // =================================================================
    // Tests cho: applyVoucherToOrder (Lấy từ File 2)
    // =================================================================
    @Nested
    @DisplayName("Tests cho hàm: applyVoucherToOrder()")
    class ApplyVoucherToOrderTests {

        // Biến và setUp cục bộ cho applyVoucherToOrder
        private Voucher validVoucher;
        private Order validOrder;
        private Event event;
        private Customer customer;

        @BeforeEach
        void setUp() {
            // Setup Event
            event = new Event();
            event.setTitle("Test Event");

            // Setup Customer
            customer = new Customer();
            customer.setCustomerId(1L);

            // Setup valid Order with originalPrice = 100000
            validOrder = new Order();
            validOrder.setOrderId(1L);
            validOrder.setOriginalPrice(new BigDecimal("100000"));
            validOrder.setEvent(event);
            validOrder.setCustomer(customer);

            // Setup valid Voucher with quantity = 5, discount = 20000
            validVoucher = new Voucher();
            validVoucher.setVoucherId(1L);
            validVoucher.setCode("VOUCHER2024");
            validVoucher.setDiscountAmount(new BigDecimal("20000"));
            validVoucher.setQuantity(5);
            validVoucher.setStatus(VoucherStatus.ACTIVE);
            validVoucher.setCreatedAt(LocalDateTime.now().minusDays(1));
            validVoucher.setExpiresAt(LocalDateTime.now().plusDays(30));
        }

        @Test
        @DisplayName("TC-01: Happy Path - Valid Voucher")
        void testApplyVoucherToOrder_HappyPath() {
            // GIVEN
            when(voucherRepo.findAvailableVoucherByCode(eq("VOUCHER2024"), any(LocalDateTime.class)))
                    .thenReturn(Optional.of(validVoucher));
            VoucherUsage mockVoucherUsage = new VoucherUsage(validVoucher, validOrder, new BigDecimal("20000"));
            mockVoucherUsage.setUsageId(1L);
            when(voucherUsageRepo.save(any(VoucherUsage.class))).thenReturn(mockVoucherUsage);
            when(voucherRepo.save(any(Voucher.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // WHEN
            VoucherUsage result = voucherService.applyVoucherToOrder("VOUCHER2024", validOrder);

            // THEN
            assertThat(result).isNotNull();
            assertThat(result.getUsageId()).isEqualTo(1L);
            assertThat(result.getDiscountApplied()).isEqualByComparingTo(new BigDecimal("20000"));
            assertThat(validOrder.getVoucher()).isEqualTo(validVoucher);
            assertThat(validVoucher.getQuantity()).isEqualTo(4);
            verify(voucherUsageRepo, times(1)).save(any(VoucherUsage.class));
            verify(voucherRepo, times(1)).save(validVoucher);
        }

        @Test
        @DisplayName("TC-02: Error - Voucher not found")
        void testApplyVoucherToOrder_VoucherNotFound() {
            // GIVEN
            when(voucherRepo.findAvailableVoucherByCode(eq("INVALID_CODE"), any(LocalDateTime.class)))
                    .thenReturn(Optional.empty());

            // WHEN + THEN
            assertThatThrownBy(() -> voucherService.applyVoucherToOrder("INVALID_CODE", validOrder))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Voucher không hợp lệ");
            verify(voucherUsageRepo, never()).save(any(VoucherUsage.class));
        }

        @Test
        @DisplayName("TC-03: Error - Out of stock (quantity=0)")
        void testApplyVoucherToOrder_OutOfStock() {
            // GIVEN
            validVoucher.setQuantity(0);
            when(voucherRepo.findAvailableVoucherByCode(eq("VOUCHER2024"), any(LocalDateTime.class)))
                    .thenReturn(Optional.of(validVoucher));

            // WHEN + THEN
            assertThatThrownBy(() -> voucherService.applyVoucherToOrder("VOUCHER2024", validOrder))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Voucher đã hết số lượng");
            verify(voucherUsageRepo, never()).save(any(VoucherUsage.class));
        }

        @Test
        @DisplayName("TC-04: Edge Case - Over-discount (Capped at order price)")
        void testApplyVoucherToOrder_OverDiscount() {
            // GIVEN
            validVoucher.setDiscountAmount(new BigDecimal("150000")); // > 100000
            when(voucherRepo.findAvailableVoucherByCode(eq("VOUCHER2024"), any(LocalDateTime.class)))
                    .thenReturn(Optional.of(validVoucher));
            VoucherUsage mockVoucherUsage = new VoucherUsage(validVoucher, validOrder, new BigDecimal("100000"));
            when(voucherUsageRepo.save(any(VoucherUsage.class))).thenReturn(mockVoucherUsage);
            when(voucherRepo.save(any(Voucher.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // WHEN
            VoucherUsage result = voucherService.applyVoucherToOrder("VOUCHER2024", validOrder);

            // THEN
            assertThat(result).isNotNull();
            // Discount bị cap = 100000 (order price)
            assertThat(result.getDiscountApplied()).isEqualByComparingTo(new BigDecimal("100000"));
        }

        @Test
        @DisplayName("TC-05: Edge Case - Concurrency simulation")
        void testApplyVoucherToOrder_ConcurrencyScenario() {
            // GIVEN: Voucher with quantity=1
            validVoucher.setQuantity(1);
            when(voucherRepo.findAvailableVoucherByCode(eq("VOUCHER2024"), any(LocalDateTime.class)))
                    .thenReturn(Optional.of(validVoucher));
            when(voucherUsageRepo.save(any(VoucherUsage.class))).thenReturn(new VoucherUsage(validVoucher, validOrder, new BigDecimal("20000")));
            when(voucherRepo.save(any(Voucher.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // WHEN: First call succeeds
            VoucherUsage firstResult = voucherService.applyVoucherToOrder("VOUCHER2024", validOrder);

            // THEN: Quantity is now 0
            assertThat(validVoucher.getQuantity()).isEqualTo(0);
            assertThat(firstResult).isNotNull();

            // Second call with quantity=0 should fail
            assertThatThrownBy(() -> voucherService.applyVoucherToOrder("VOUCHER2024", validOrder))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Voucher đã hết số lượng");

            verify(voucherUsageRepo, times(1)).save(any(VoucherUsage.class));
            verify(voucherRepo, times(1)).save(validVoucher);
        }

        @Test
        @DisplayName("TC-06: Integration - Full flow with ArgumentCaptor")
        void testApplyVoucherToOrder_FullIntegrationFlow() {
            // GIVEN
            when(voucherRepo.findAvailableVoucherByCode(eq("VOUCHER2024"), any(LocalDateTime.class)))
                    .thenReturn(Optional.of(validVoucher));

            ArgumentCaptor<VoucherUsage> voucherUsageCaptor = ArgumentCaptor.forClass(VoucherUsage.class);
            when(voucherUsageRepo.save(voucherUsageCaptor.capture())).thenAnswer(i -> i.getArgument(0));

            ArgumentCaptor<Voucher> voucherCaptor = ArgumentCaptor.forClass(Voucher.class);
            when(voucherRepo.save(voucherCaptor.capture())).thenAnswer(i -> i.getArgument(0));

            // WHEN
            voucherService.applyVoucherToOrder("VOUCHER2024", validOrder);

            // THEN: Verify order was updated
            assertThat(validOrder.getVoucher()).isEqualTo(validVoucher);
            assertThat(validOrder.getVoucherDiscountAmount()).isEqualByComparingTo(new BigDecimal("20000"));

            // THEN: Verify voucher was updated
            Voucher savedVoucher = voucherCaptor.getValue();
            assertThat(savedVoucher.getQuantity()).isEqualTo(4);

            // THEN: Verify VoucherUsage was saved
            VoucherUsage savedUsage = voucherUsageCaptor.getValue();
            assertThat(savedUsage.getVoucher()).isEqualTo(validVoucher);
            assertThat(savedUsage.getOrder()).isEqualTo(validOrder);
            assertThat(savedUsage.getDiscountApplied()).isEqualByComparingTo(new BigDecimal("20000"));
        }
    }

    // =================================================================
    // Tests cho các hàm khác (Lấy từ File 1)
    // Các test này sẽ sử dụng 'generalValidVoucher' và 'generalValidOrder'
    // =================================================================

    @Nested
    @DisplayName("Tests cho hàm: calculateVoucherDiscount()")
    class CalculateVoucherDiscountScenarios {

        @Test
        @DisplayName("Happy Path - Trả về đúng discount")
        void givenValidVoucherCode_whenCalculateDiscount_thenShouldReturnCorrectDiscountAmount() {
            // Given
            when(voucherRepo.findAvailableVoucherByCode(eq(generalValidVoucherCode), any(LocalDateTime.class)))
                    .thenReturn(Optional.of(generalValidVoucher));

            // When
            BigDecimal result = voucherService.calculateVoucherDiscount(generalValidVoucherCode, new BigDecimal("500.00"));

            // Then
            assertThat(result).isEqualTo(generalValidVoucher.getDiscountAmount()); // 50.00
        }

        @Test
        @DisplayName("Edge Case - Discount bị cap bằng giá order")
        void givenVoucherDiscountGreaterThanOrderPrice_whenCalculateDiscount_thenShouldReturnOrderPrice() {
            // Given
            generalValidVoucher.setDiscountAmount(new BigDecimal("1000.00")); // > 500.00
            when(voucherRepo.findAvailableVoucherByCode(eq(generalValidVoucherCode), any(LocalDateTime.class)))
                    .thenReturn(Optional.of(generalValidVoucher));

            // When
            BigDecimal result = voucherService.calculateVoucherDiscount(generalValidVoucherCode, new BigDecimal("500.00"));

            // Then: Should return order price (capped)
            assertThat(result).isEqualTo(new BigDecimal("500.00"));
        }

        @Test
        @DisplayName("Error - Voucher không hợp lệ, trả về ZERO")
        void givenInvalidVoucherCode_whenCalculateDiscount_thenShouldReturnZero() {
            // Given
            String invalidVoucherCode = "INVALID_CODE";
            when(voucherRepo.findAvailableVoucherByCode(eq(invalidVoucherCode), any(LocalDateTime.class)))
                    .thenReturn(Optional.empty());

            // When
            BigDecimal result = voucherService.calculateVoucherDiscount(invalidVoucherCode, new BigDecimal("500.00"));

            // Then
            assertThat(result).isEqualTo(BigDecimal.ZERO);
        }
    }

    @Nested
    @DisplayName("Tests cho hàm: isVoucherAvailable()")
    class IsVoucherAvailableScenarios {

        @Test
        @DisplayName("Voucher hợp lệ, trả về true")
        void givenAvailableVoucherCode_whenCheckingAvailability_thenShouldReturnTrue() {
            // Given
            when(voucherRepo.findAvailableVoucherByCode(eq(generalValidVoucherCode), any(LocalDateTime.class)))
                    .thenReturn(Optional.of(generalValidVoucher));

            // When
            boolean result = voucherService.isVoucherAvailable(generalValidVoucherCode);

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Voucher không hợp lệ, trả về false")
        void givenUnavailableVoucherCode_whenCheckingAvailability_thenShouldReturnFalse() {
            // Given
            String unavailableVoucherCode = "UNAVAILABLE";
            when(voucherRepo.findAvailableVoucherByCode(eq(unavailableVoucherCode), any(LocalDateTime.class)))
                    .thenReturn(Optional.empty());

            // When
            boolean result = voucherService.isVoucherAvailable(unavailableVoucherCode);

            // Then
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("Tests cho hàm: getVoucherByCode()")
    class GetVoucherByCodeScenarios {

        @Test
        @DisplayName("Code tồn tại, trả về Optional<Voucher>")
        void givenExistingVoucherCode_whenGettingVoucher_thenShouldReturnVoucher() {
            // Given
            when(voucherRepo.findByCode(generalValidVoucherCode))
                    .thenReturn(Optional.of(generalValidVoucher));

            // When
            Optional<Voucher> result = voucherService.getVoucherByCode(generalValidVoucherCode);

            // Then
            assertThat(result).isPresent();
            assertThat(result.get()).isEqualTo(generalValidVoucher);
        }

        @Test
        @DisplayName("Code không tồn tại, trả về Optional.empty")
        void givenNonExistingVoucherCode_whenGettingVoucher_thenShouldReturnEmpty() {
            // Given
            String nonExistingCode = "NON_EXISTING";
            when(voucherRepo.findByCode(nonExistingCode))
                    .thenReturn(Optional.empty());

            // When
            Optional<Voucher> result = voucherService.getVoucherByCode(nonExistingCode);

            // Then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("Tests cho hàm: getAvailableVouchers()")
    class GetAvailableVouchersScenarios {

        @Test
        @DisplayName("Khi có voucher, trả về List<Voucher>")
        void givenAvailableVouchersExist_whenGettingAvailableVouchers_thenShouldReturnListOfVouchers() {
            // Given
            List<Voucher> availableVouchers = Arrays.asList(generalValidVoucher, createGeneralValidVoucher());
            when(voucherRepo.findAllAvailableVouchers(any(LocalDateTime.class)))
                    .thenReturn(availableVouchers);

            // When
            List<Voucher> result = voucherService.getAvailableVouchers();

            // Then
            assertThat(result).hasSize(2);
            assertThat(result).containsExactlyElementsOf(availableVouchers);
        }

        @Test
        @DisplayName("Khi không có voucher, trả về List rỗng")
        void givenNoAvailableVouchers_whenGettingAvailableVouchers_thenShouldReturnEmptyList() {
            // Given
            when(voucherRepo.findAllAvailableVouchers(any(LocalDateTime.class)))
                    .thenReturn(Arrays.asList());

            // When
            List<Voucher> result = voucherService.getAvailableVouchers();

            // Then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("Tests cho hàm: createVoucher()")
    class CreateVoucherScenarios {

        @Test
        @DisplayName("Tạo thành công, trả về voucher đã lưu")
        void givenValidVoucher_whenCreatingVoucher_thenShouldSaveAndReturnVoucher() {
            // Given
            Voucher newVoucher = createGeneralValidVoucher();
            newVoucher.setVoucherId(null);
            when(voucherRepo.save(any(Voucher.class)))
                    .thenAnswer(invocation -> {
                        Voucher voucher = invocation.getArgument(0);
                        voucher.setVoucherId(1L);
                        return voucher;
                    });

            // When
            Voucher result = voucherService.createVoucher(newVoucher);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getVoucherId()).isEqualTo(1L);
            verify(voucherRepo, times(1)).save(newVoucher);
        }
    }

    @Nested
    @DisplayName("Tests cho hàm: disableVoucher()")
    class DisableVoucherScenarios {

        @Test
        @DisplayName("Disable thành công, trả về true")
        void givenExistingVoucherId_whenDisablingVoucher_thenShouldDisableAndReturnTrue() {
            // Given
            Long voucherId = 1L;
            when(voucherRepo.findById(voucherId))
                    .thenReturn(Optional.of(generalValidVoucher));
            when(voucherRepo.save(any(Voucher.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            boolean result = voucherService.disableVoucher(voucherId);

            // Then
            assertThat(result).isTrue();
            assertThat(generalValidVoucher.getStatus()).isEqualTo(VoucherStatus.DISABLED);
            verify(voucherRepo, times(1)).save(generalValidVoucher);
        }

        @Test
        @DisplayName("ID không tồn tại, trả về false")
        void givenNonExistingVoucherId_whenDisablingVoucher_thenShouldReturnFalse() {
            // Given
            Long nonExistingId = 999L;
            when(voucherRepo.findById(nonExistingId))
                    .thenReturn(Optional.empty());

            // When
            boolean result = voucherService.disableVoucher(nonExistingId);

            // Then
            assertThat(result).isFalse();
            verify(voucherRepo, never()).save(any(Voucher.class));
        }
    }

    @Nested
    @DisplayName("Tests cho hàm: updateVoucherQuantity()")
    class UpdateVoucherQuantityScenarios {

        @Test
        @DisplayName("Update thành công, trả về voucher đã update")
        void givenExistingVoucherIdAndNewQuantity_whenUpdatingQuantity_thenShouldUpdateAndReturnVoucher() {
            // Given
            Long voucherId = 1L;
            Integer newQuantity = 10;
            when(voucherRepo.findById(voucherId))
                    .thenReturn(Optional.of(generalValidVoucher));
            when(voucherRepo.save(any(Voucher.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            Voucher result = voucherService.updateVoucherQuantity(voucherId, newQuantity);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getQuantity()).isEqualTo(newQuantity);
            verify(voucherRepo, times(1)).save(generalValidVoucher);
        }

        @Test
        @DisplayName("ID không tồn tại, ném IllegalArgumentException")
        void givenNonExistingVoucherId_whenUpdatingQuantity_thenShouldThrowIllegalArgumentException() {
            // Given
            Long nonExistingId = 999L;
            when(voucherRepo.findById(nonExistingId))
                    .thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> voucherService.updateVoucherQuantity(nonExistingId, 10))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Voucher không tồn tại");
        }
    }

    @Nested
    @DisplayName("Tests cho hàm: getVoucherUsageHistory()")
    class GetVoucherUsageHistoryScenarios {

        @Test
        @DisplayName("Voucher có lịch sử, trả về List<VoucherUsage>")
        void givenVoucherIdWithUsageHistory_whenGettingUsageHistory_thenShouldReturnListOfVoucherUsages() {
            // Given
            Long voucherId = 1L;
            List<VoucherUsage> usageHistory = Arrays.asList(
                    new VoucherUsage(generalValidVoucher, generalValidOrder, new BigDecimal("50.00"))
            );
            when(voucherUsageRepo.findByVoucherVoucherId(voucherId))
                    .thenReturn(usageHistory);

            // When
            List<VoucherUsage> result = voucherService.getVoucherUsageHistory(voucherId);

            // Then
            assertThat(result).hasSize(1);
            assertThat(result).containsExactlyElementsOf(usageHistory);
        }

        @Test
        @DisplayName("Voucher không có lịch sử, trả về List rỗng")
        void givenVoucherIdWithNoUsageHistory_whenGettingUsageHistory_thenShouldReturnEmptyList() {
            // Given
            Long voucherId = 1L;
            when(voucherUsageRepo.findByVoucherVoucherId(voucherId))
                    .thenReturn(Arrays.asList());

            // When
            List<VoucherUsage> result = voucherService.getVoucherUsageHistory(voucherId);

            // Then
            assertThat(result).isEmpty();
        }
    }

    // =================================================================
    // Helper methods (dùng chung cho các test bên ngoài)
    // =================================================================

    private Voucher createGeneralValidVoucher() {
        Voucher voucher = new Voucher();
        voucher.setVoucherId(1L);
        voucher.setCode("SAVE20");
        voucher.setDiscountAmount(new BigDecimal("50.00")); // 50
        voucher.setQuantity(5);
        voucher.setStatus(VoucherStatus.ACTIVE);
        voucher.setCreatedAt(LocalDateTime.now().minusDays(1));
        voucher.setExpiresAt(LocalDateTime.now().plusDays(30));
        voucher.setDescription("20% discount voucher");
        return voucher;
    }

    private Order createGeneralValidOrder() {
        Order order = new Order();
        order.setOrderId(1L);
        order.setOriginalPrice(new BigDecimal("500.00")); // 500
        order.setTotalAmount(new BigDecimal("500.00"));
        return order;
    }
}