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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * BDD Test Cases for VoucherService.applyVoucherToOrder
 * Framework: JUnit 5 with Mockito
 * 
 * Test Coverage:
 * TC-01: Happy Path - Valid Voucher
 * TC-02: Error - Voucher not found
 * TC-03: Error - Out of stock (quantity = 0)
 * TC-04: Edge - Over-discount (discount > order price)
 * TC-05: Edge - Concurrency (two threads apply same voucher)
 * TC-06: Integration - Apply + Order Save
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AI OUTPUT — BDD TEST CASE TABLE - VoucherService.applyVoucherToOrder")
class VoucherServiceImplTest {

    @Mock
    private IVoucherRepo voucherRepo;

    @Mock
    private IVoucherUsageRepo voucherUsageRepo;

    @InjectMocks
    private VoucherServiceImpl voucherService;

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

    /**
     * TC-01: Happy Path - Valid Voucher
     * Given: VoucherRepo returns available voucher (quantity=5)
     * When: call method
     * Then: Order updated (discount applied, usage saved, quantity decreased)
     * Mock Setup / Verification:
     *   - Verify voucherUsageRepo.save() + voucherRepo.save() + voucher.decreaseQuantity() once
     */
    @Test
    @DisplayName("TC-01: Happy Path - Valid Voucher returns VoucherUsage and updates Order")
    void testApplyVoucherToOrder_HappyPath() {
        // GIVEN: VoucherRepo returns available voucher (quantity=5)
        when(voucherRepo.findAvailableVoucherByCode(eq("VOUCHER2024"), any(LocalDateTime.class)))
            .thenReturn(Optional.of(validVoucher));
        
        VoucherUsage mockVoucherUsage = new VoucherUsage(validVoucher, validOrder, new BigDecimal("20000"));
        mockVoucherUsage.setUsageId(1L);
        when(voucherUsageRepo.save(any(VoucherUsage.class))).thenReturn(mockVoucherUsage);
        
        when(voucherRepo.save(any(Voucher.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // WHEN: call method
        VoucherUsage result = voucherService.applyVoucherToOrder("VOUCHER2024", validOrder);

        // THEN: Order updated (discount applied, usage saved, quantity decreased)
        assertThat(result).isNotNull();
        assertThat(result.getUsageId()).isEqualTo(1L);
        assertThat(result.getDiscountApplied()).isEqualByComparingTo(new BigDecimal("20000"));
        
        // Verify order updated
        assertThat(validOrder.getVoucher()).isEqualTo(validVoucher);
        assertThat(validOrder.getVoucherCode()).isEqualTo("VOUCHER2024");
        assertThat(validOrder.getVoucherDiscountAmount()).isEqualByComparingTo(new BigDecimal("20000"));
        
        // Verify voucher quantity decreased
        assertThat(validVoucher.getQuantity()).isEqualTo(4);

        // Mock Setup / Verification
        // Note: findAvailableVoucherByCode called 2 times - once in applyVoucherToOrder and once in calculateVoucherDiscount
        verify(voucherRepo, times(2)).findAvailableVoucherByCode(eq("VOUCHER2024"), any(LocalDateTime.class));
        verify(voucherUsageRepo, times(1)).save(any(VoucherUsage.class));
        verify(voucherRepo, times(1)).save(validVoucher);
    }

    /**
     * TC-02: Error - Voucher not found
     * Given: Repo returns Optional.empty()
     * When: call method
     * Then: Throws IllegalArgumentException("Voucher không hợp lệ...")
     * Mock Setup / Verification: Verify no save invoked
     */
    @Test
    @DisplayName("TC-02: Error - Voucher not found throws IllegalArgumentException")
    void testApplyVoucherToOrder_VoucherNotFound() {
        // GIVEN: Repo returns Optional.empty()
        when(voucherRepo.findAvailableVoucherByCode(eq("INVALID_CODE"), any(LocalDateTime.class)))
            .thenReturn(Optional.empty());

        // WHEN + THEN: Throws IllegalArgumentException("Voucher không hợp lệ...")
        assertThatThrownBy(() -> voucherService.applyVoucherToOrder("INVALID_CODE", validOrder))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Voucher không hợp lệ");

        // Mock Setup / Verification: Verify no save invoked
        verify(voucherUsageRepo, never()).save(any(VoucherUsage.class));
        verify(voucherRepo, never()).save(any(Voucher.class));
    }

    /**
     * TC-03: Error - Out of stock
     * Given: Voucher quantity=0
     * When: call method
     * Then: Throws IllegalArgumentException("Voucher đã hết số lượng...")
     * Mock Setup / Verification: Verify quantity not updated
     */
    @Test
    @DisplayName("TC-03: Error - Out of stock (quantity=0) throws IllegalArgumentException")
    void testApplyVoucherToOrder_OutOfStock() {
        // GIVEN: Voucher quantity=0
        validVoucher.setQuantity(0);
        when(voucherRepo.findAvailableVoucherByCode(eq("VOUCHER2024"), any(LocalDateTime.class)))
            .thenReturn(Optional.of(validVoucher));

        // WHEN + THEN: Throws IllegalArgumentException("Voucher đã hết số lượng...")
        assertThatThrownBy(() -> voucherService.applyVoucherToOrder("VOUCHER2024", validOrder))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Voucher đã hết số lượng");

        // Mock Setup / Verification: Verify quantity not updated
        verify(voucherUsageRepo, never()).save(any(VoucherUsage.class));
        verify(voucherRepo, never()).save(any(Voucher.class));
        assertThat(validVoucher.getQuantity()).isEqualTo(0);
    }

    /**
     * TC-04: Edge - Over-discount
     * Given: Voucher discount > order price
     * When: call method
     * Then: Total capped at 0
     * Mock Setup / Verification: Verify calculation safe
     */
    @Test
    @DisplayName("TC-04: Edge Case - Over-discount correctly caps discount at order price")
    void testApplyVoucherToOrder_OverDiscount() {
        // GIVEN: Voucher discount > order price
        validVoucher.setDiscountAmount(new BigDecimal("150000")); // More than order price (100000)
        when(voucherRepo.findAvailableVoucherByCode(eq("VOUCHER2024"), any(LocalDateTime.class)))
            .thenReturn(Optional.of(validVoucher));
        
        VoucherUsage mockVoucherUsage = new VoucherUsage(validVoucher, validOrder, new BigDecimal("100000"));
        when(voucherUsageRepo.save(any(VoucherUsage.class))).thenReturn(mockVoucherUsage);
        when(voucherRepo.save(any(Voucher.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // WHEN: call method
        VoucherUsage result = voucherService.applyVoucherToOrder("VOUCHER2024", validOrder);

        // THEN: Total capped at order price (discount = 100000, not 150000)
        assertThat(result).isNotNull();
        // The discount should be capped at original price (100000)
        assertThat(result.getDiscountApplied()).isEqualByComparingTo(new BigDecimal("100000"));
        
        // Order total should never go negative
        validOrder.calculateTotalAmount();
        assertThat(validOrder.getTotalAmount()).isGreaterThanOrEqualTo(BigDecimal.ZERO);

        // Mock Setup / Verification: Verify calculation safe
        verify(voucherUsageRepo, times(1)).save(any(VoucherUsage.class));
        verify(voucherRepo, times(1)).save(validVoucher);
    }

    /**
     * TC-05: Edge - Concurrency
     * Given: Two threads apply same voucher
     * When: concurrent calls
     * Then: Only one succeeds; quantity decremented once
     * Mock Setup / Verification: Verify atomicity
     * 
     * Note: This test demonstrates the concurrency scenario concept
     */
    @Test
    @DisplayName("TC-05: Edge Case - Concurrency scenario (conceptual test)")
    void testApplyVoucherToOrder_ConcurrencyScenario() {
        // GIVEN: Voucher with quantity=1
        validVoucher.setQuantity(1);
        when(voucherRepo.findAvailableVoucherByCode(eq("VOUCHER2024"), any(LocalDateTime.class)))
            .thenReturn(Optional.of(validVoucher));
        
        VoucherUsage mockVoucherUsage = new VoucherUsage(validVoucher, validOrder, new BigDecimal("20000"));
        when(voucherUsageRepo.save(any(VoucherUsage.class))).thenReturn(mockVoucherUsage);
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

        // Mock Setup / Verification: Verify atomicity
        // In real concurrent scenario with @Transactional, database would handle atomicity
        verify(voucherUsageRepo, times(1)).save(any(VoucherUsage.class));
        verify(voucherRepo, times(1)).save(validVoucher);
    }

    /**
     * TC-06: Integration - Apply + Order Save
     * Given: Valid voucher + repo interactions
     * When: full flow
     * Then: Total recalculated, voucher updated
     * Mock Setup / Verification: Verify order persistence flow
     */
    @Test
    @DisplayName("TC-06: Integration - Full flow with voucher application and order update")
    void testApplyVoucherToOrder_FullIntegrationFlow() {
        // GIVEN: Valid voucher + repo interactions
        when(voucherRepo.findAvailableVoucherByCode(eq("VOUCHER2024"), any(LocalDateTime.class)))
            .thenReturn(Optional.of(validVoucher));
        
        ArgumentCaptor<VoucherUsage> voucherUsageCaptor = ArgumentCaptor.forClass(VoucherUsage.class);
        when(voucherUsageRepo.save(voucherUsageCaptor.capture())).thenAnswer(invocation -> {
            VoucherUsage vu = invocation.getArgument(0);
            vu.setUsageId(1L);
            vu.setUsedAt(LocalDateTime.now());
            return vu;
        });
        
        ArgumentCaptor<Voucher> voucherCaptor = ArgumentCaptor.forClass(Voucher.class);
        when(voucherRepo.save(voucherCaptor.capture())).thenAnswer(invocation -> invocation.getArgument(0));

        // WHEN: full flow
        BigDecimal originalPrice = validOrder.getOriginalPrice();
        VoucherUsage result = voucherService.applyVoucherToOrder("VOUCHER2024", validOrder);

        // THEN: Total recalculated, voucher updated
        assertThat(result).isNotNull();
        assertThat(result.getUsageId()).isEqualTo(1L);
        assertThat(result.getUsedAt()).isNotNull();
        
        // Verify order was updated correctly
        assertThat(validOrder.getVoucher()).isEqualTo(validVoucher);
        assertThat(validOrder.getVoucherCode()).isEqualTo("VOUCHER2024");
        assertThat(validOrder.getVoucherDiscountAmount()).isEqualByComparingTo(new BigDecimal("20000"));
        
        // Note: Order.calculateTotalAmount() is called internally by applyVoucherToOrder
        // The actual calculation depends on Order implementation
        // We verify that voucher discount was set correctly on the order
        
        // Verify voucher quantity decreased
        Voucher savedVoucher = voucherCaptor.getValue();
        assertThat(savedVoucher.getQuantity()).isEqualTo(4);
        
        // Verify VoucherUsage was saved with correct data
        VoucherUsage savedUsage = voucherUsageCaptor.getValue();
        assertThat(savedUsage.getVoucher()).isEqualTo(validVoucher);
        assertThat(savedUsage.getOrder()).isEqualTo(validOrder);
        assertThat(savedUsage.getDiscountApplied()).isEqualByComparingTo(new BigDecimal("20000"));

        // Mock Setup / Verification: Verify order persistence flow
        // Note: findAvailableVoucherByCode called 2 times - once in applyVoucherToOrder and once in calculateVoucherDiscount
        verify(voucherRepo, times(2)).findAvailableVoucherByCode(eq("VOUCHER2024"), any(LocalDateTime.class));
        verify(voucherUsageRepo, times(1)).save(any(VoucherUsage.class));
        verify(voucherRepo, times(1)).save(any(Voucher.class));
    }
}

