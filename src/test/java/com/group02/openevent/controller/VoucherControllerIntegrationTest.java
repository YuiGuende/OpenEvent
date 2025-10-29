package com.group02.openevent.controller;

import com.group02.openevent.model.voucher.Voucher;
import com.group02.openevent.service.VoucherService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class VoucherControllerIntegrationTest {

    @InjectMocks
    private VoucherController voucherController;

    @Mock private VoucherService voucherService;

    private MockMvc mockMvc;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(voucherController)
                .build();
    }

    @Test
    void shouldReturn401WhenNoCurrentUserIdForValidateVoucher() throws Exception {
        mockMvc.perform(get("/api/vouchers/validate/SAVE10"))
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("User not logged in"));
    }

    @Test
    void shouldReturn401WhenNoCurrentUserIdForGetAvailableVouchers() throws Exception {
        mockMvc.perform(get("/api/vouchers/available"))
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("User not logged in"));
    }

    @Test
    void shouldReturnValidVoucherWhenVoucherExistsAndAvailable() throws Exception {
        String voucherCode = "SAVE10";
        Voucher voucher = new Voucher();
        voucher.setCode(voucherCode);
        voucher.setDiscountAmount(new BigDecimal("10000"));
        voucher.setDescription("Giảm 10,000 VND");

        when(voucherService.isVoucherAvailable(voucherCode)).thenReturn(true);
        when(voucherService.getVoucherByCode(voucherCode)).thenReturn(Optional.of(voucher));

        mockMvc.perform(get("/api/vouchers/validate/{voucherCode}", voucherCode)
                        .requestAttr("currentUserId", 1L))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.voucher.code").value(voucherCode))
                .andExpect(jsonPath("$.voucher.discountAmount").value(10000))
                .andExpect(jsonPath("$.voucher.description").value("Giảm 10,000 VND"));
    }

    @Test
    void shouldReturnInvalidVoucherWhenVoucherNotAvailable() throws Exception {
        String voucherCode = "INVALID";
        
        when(voucherService.isVoucherAvailable(voucherCode)).thenReturn(false);

        mockMvc.perform(get("/api/vouchers/validate/{voucherCode}", voucherCode)
                        .requestAttr("currentUserId", 1L))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Mã voucher không hợp lệ hoặc đã hết hạn"));
    }

    @Test
    void shouldReturnInvalidVoucherWhenVoucherNotFound() throws Exception {
        String voucherCode = "NOTFOUND";
        
        when(voucherService.isVoucherAvailable(voucherCode)).thenReturn(true);
        when(voucherService.getVoucherByCode(voucherCode)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/vouchers/validate/{voucherCode}", voucherCode)
                        .requestAttr("currentUserId", 1L))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Mã voucher không hợp lệ hoặc đã hết hạn"));
    }

    @Test
    void shouldReturnAvailableVouchers() throws Exception {
        Voucher voucher1 = new Voucher();
        voucher1.setCode("SAVE10");
        voucher1.setDiscountAmount(new BigDecimal("10000"));
        voucher1.setDescription("Giảm 10,000 VND");

        Voucher voucher2 = new Voucher();
        voucher2.setCode("SAVE20");
        voucher2.setDiscountAmount(new BigDecimal("20000"));
        voucher2.setDescription("Giảm 20,000 VND");

        List<Voucher> vouchers = Arrays.asList(voucher1, voucher2);
        when(voucherService.getAvailableVouchers()).thenReturn(vouchers);

        mockMvc.perform(get("/api/vouchers/available")
                        .requestAttr("currentUserId", 1L))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.vouchers").isArray())
                .andExpect(jsonPath("$.vouchers.length()").value(2))
                .andExpect(jsonPath("$.vouchers[0].code").value("SAVE10"))
                .andExpect(jsonPath("$.vouchers[1].code").value("SAVE20"));
    }

    @Test
    void shouldHandleExceptionInValidateVoucher() throws Exception {
        String voucherCode = "ERROR";
        
        when(voucherService.isVoucherAvailable(voucherCode)).thenThrow(new RuntimeException("Database error"));

        mockMvc.perform(get("/api/vouchers/validate/{voucherCode}", voucherCode)
                        .requestAttr("currentUserId", 1L))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Database error"));
    }

    @Test
    void shouldHandleExceptionInGetAvailableVouchers() throws Exception {
        when(voucherService.getAvailableVouchers()).thenThrow(new RuntimeException("Service error"));

        mockMvc.perform(get("/api/vouchers/available")
                        .requestAttr("currentUserId", 1L))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Service error"));
    }
}



