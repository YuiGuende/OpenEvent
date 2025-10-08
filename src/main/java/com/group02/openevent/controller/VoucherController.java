package com.group02.openevent.controller;

import com.group02.openevent.model.voucher.Voucher;
import com.group02.openevent.service.VoucherService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/vouchers")
public class VoucherController {
    
    @Autowired
    private VoucherService voucherService;
    
    /**
     * Validate voucher code for frontend
     */
    @GetMapping("/validate/{voucherCode}")
    public ResponseEntity<?> validateVoucher(@PathVariable String voucherCode, HttpServletRequest httpRequest) {
        Long accountId = (Long) httpRequest.getAttribute("currentUserId");
        if (accountId == null) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "User not logged in"));
        }

        try {
            boolean isAvailable = voucherService.isVoucherAvailable(voucherCode);
            if (isAvailable) {
                Optional<Voucher> voucherOpt = voucherService.getVoucherByCode(voucherCode);
                if (voucherOpt.isPresent()) {
                    Voucher voucher = voucherOpt.get();
                    return ResponseEntity.ok(Map.of(
                        "success", true,
                        "voucher", Map.of(
                            "code", voucher.getCode(),
                            "discountAmount", voucher.getDiscountAmount(),
                            "description", voucher.getDescription()
                        )
                    ));
                }
            }
            
            return ResponseEntity.ok(Map.of(
                "success", false,
                "message", "Mã voucher không hợp lệ hoặc đã hết hạn"
            ));
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false, 
                "message", e.getMessage()
            ));
        }
    }
    
    /**
     * Get available vouchers
     */
    @GetMapping("/available")
    public ResponseEntity<?> getAvailableVouchers(HttpServletRequest httpRequest) {
        Long accountId = (Long) httpRequest.getAttribute("currentUserId");
        if (accountId == null) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "User not logged in"));
        }

        try {
            var vouchers = voucherService.getAvailableVouchers();
            return ResponseEntity.ok(Map.of("success", true, "vouchers", vouchers));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }
}
