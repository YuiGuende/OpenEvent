package com.group02.openevent.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.group02.openevent.config.SessionInterceptor;
import com.group02.openevent.model.voucher.Voucher;
import com.group02.openevent.model.voucher.VoucherStatus;
import com.group02.openevent.service.VoucherService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Lớp test cho VoucherController, đã gộp và refactor.
 * Sử dụng @WebMvcTest và @Nested
 * Áp dụng logic xác thực (401, 400) từ file IntegrationTest cũ.
 */
@WebMvcTest(
        controllers = VoucherController.class,
        // Loại bỏ Security mặc định của Spring Boot
        excludeAutoConfiguration = SecurityAutoConfiguration.class
)
class VoucherControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private VoucherService voucherService;

    @MockitoBean
    private SessionInterceptor sessionInterceptor; // Mock để context load thành công

    private Voucher sampleVoucher;
    private final String VALID_VOUCHER_CODE = "SAVE10";
    private final Long TEST_USER_ID = 1L;

    @BeforeEach
    void setUp() {
        // Mock Interceptor để cho phép request đi qua (nếu cần)
        // Trong các test 401, chúng ta sẽ không set requestAttr,
        // nên controller sẽ tự trả về 401
        try {
            when(sessionInterceptor.preHandle(any(), any(), any())).thenReturn(true);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // Setup voucher mẫu
        sampleVoucher = new Voucher();
        sampleVoucher.setVoucherId(1L);
        sampleVoucher.setCode(VALID_VOUCHER_CODE);
        sampleVoucher.setDiscountAmount(new BigDecimal("10000"));
        sampleVoucher.setDescription("Giảm 10,000 VND");
        sampleVoucher.setStatus(VoucherStatus.ACTIVE);
        sampleVoucher.setExpiresAt(LocalDateTime.now().plusDays(30));
    }

    // =================================================================
    // Tests cho: validateVoucher()
    // =================================================================
    @Nested
    @DisplayName("Tests cho hàm: validateVoucher() - GET /api/vouchers/validate/{voucherCode}")
    class ValidateVoucherTests {

        @Test
        @DisplayName("V-SEC-001: Khi không đăng nhập, trả về 401 Unauthorized")
        void shouldReturn401WhenNoCurrentUserIdForValidateVoucher() throws Exception {
            // Không set .requestAttr("currentUserId", ...)
            mockMvc.perform(get("/api/vouchers/validate/{voucherCode}", VALID_VOUCHER_CODE))
                    .andDo(print())
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("User not logged in"));
        }

        @Test
        @DisplayName("V-HAPPY-001: Voucher hợp lệ, trả về 200 OK và thông tin voucher")
        void shouldReturnValidVoucherWhenVoucherExistsAndAvailable() throws Exception {
            when(voucherService.isVoucherAvailable(VALID_VOUCHER_CODE)).thenReturn(true);
            when(voucherService.getVoucherByCode(VALID_VOUCHER_CODE)).thenReturn(Optional.of(sampleVoucher));

            mockMvc.perform(get("/api/vouchers/validate/{voucherCode}", VALID_VOUCHER_CODE)
                            .requestAttr("currentUserId", TEST_USER_ID)) // Đã đăng nhập
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.voucher.code").value(VALID_VOUCHER_CODE))
                    .andExpect(jsonPath("$.voucher.discountAmount").value(10000))
                    .andExpect(jsonPath("$.voucher.description").value("Giảm 10,000 VND"));
        }

        @Test
        @DisplayName("V-ERR-001: Voucher không available (isAvailable=false), trả về 200 OK và success=false")
        void shouldReturnInvalidVoucherWhenVoucherNotAvailable() throws Exception {
            String voucherCode = "INVALID";

            when(voucherService.isVoucherAvailable(voucherCode)).thenReturn(false);

            mockMvc.perform(get("/api/vouchers/validate/{voucherCode}", voucherCode)
                            .requestAttr("currentUserId", TEST_USER_ID))
                    .andDo(print())
                    .andExpect(status().isOk()) // Logic nghiệp vụ trả 200
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Mã voucher không hợp lệ hoặc đã hết hạn"));
        }

        @Test
        @DisplayName("V-ERR-002: Voucher không tìm thấy (getVoucherByCode=empty), trả về 200 OK và success=false")
        void shouldReturnInvalidVoucherWhenVoucherNotFound() throws Exception {
            String voucherCode = "NOTFOUND";

            when(voucherService.isVoucherAvailable(voucherCode)).thenReturn(true);
            when(voucherService.getVoucherByCode(voucherCode)).thenReturn(Optional.empty());

            mockMvc.perform(get("/api/vouchers/validate/{voucherCode}", voucherCode)
                            .requestAttr("currentUserId", TEST_USER_ID))
                    .andDo(print())
                    .andExpect(status().isOk()) // Logic nghiệp vụ trả 200
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Mã voucher không hợp lệ hoặc đã hết hạn"));
        }

        @Test
        @DisplayName("V-ERR-003: Service ném Exception, trả về 400 Bad Request")
        void shouldHandleExceptionInValidateVoucher() throws Exception {
            String voucherCode = "ERROR";

            when(voucherService.isVoucherAvailable(voucherCode)).thenThrow(new RuntimeException("Database error"));

            mockMvc.perform(get("/api/vouchers/validate/{voucherCode}", voucherCode)
                            .requestAttr("currentUserId", TEST_USER_ID))
                    .andDo(print())
                    .andExpect(status().isBadRequest()) // Lỗi hệ thống trả 400
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Database error"));
        }
    }

    // =================================================================
    // Tests cho: getAvailableVouchers()
    // =================================================================
    @Nested
    @DisplayName("Tests cho hàm: getAvailableVouchers() - GET /api/vouchers/available")
    class GetAvailableVouchersTests {

        @Test
        @DisplayName("A-SEC-001: Khi không đăng nhập, trả về 401 Unauthorized")
        void shouldReturn401WhenNoCurrentUserIdForGetAvailableVouchers() throws Exception {
            mockMvc.perform(get("/api/vouchers/available"))
                    .andDo(print())
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("User not logged in"));
        }

        @Test
        @DisplayName("A-HAPPY-001: Trả về danh sách voucher, 200 OK")
        void shouldReturnAvailableVouchers() throws Exception {
            Voucher voucher2 = new Voucher();
            voucher2.setCode("SAVE20");
            voucher2.setDiscountAmount(new BigDecimal("20000"));
            voucher2.setDescription("Giảm 20,000 VND");

            List<Voucher> vouchers = Arrays.asList(sampleVoucher, voucher2);
            when(voucherService.getAvailableVouchers()).thenReturn(vouchers);

            mockMvc.perform(get("/api/vouchers/available")
                            .requestAttr("currentUserId", TEST_USER_ID))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.vouchers").isArray())
                    .andExpect(jsonPath("$.vouchers.length()").value(2))
                    .andExpect(jsonPath("$.vouchers[0].code").value("SAVE10"))
                    .andExpect(jsonPath("$.vouchers[1].code").value("SAVE20"));
        }

        @Test
        @DisplayName("A-ERR-001: Service ném Exception, trả về 400 Bad Request")
        void shouldHandleExceptionInGetAvailableVouchers() throws Exception {
            when(voucherService.getAvailableVouchers()).thenThrow(new RuntimeException("Service error"));

            mockMvc.perform(get("/api/vouchers/available")
                            .requestAttr("currentUserId", TEST_USER_ID))
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Service error"));
        }
    }
}