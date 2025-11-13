package com.group02.openevent.service.impl;

import com.group02.openevent.service.BankVerificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.regex.Pattern;

/**
 * Mock implementation của BankVerificationService
 * Mock logic: 
 * - STK 123 + MB -> NGUYEN TRAN THANH DUY
 * - STK 321 + MB -> LE HUYNH DUC
 * - Các trường hợp khác -> null (không hợp lệ)
 */
@Service
public class BankVerificationServiceImpl implements BankVerificationService {

    private static final Logger logger = LoggerFactory.getLogger(BankVerificationServiceImpl.class);

    @Override
    public String verifyBankAccount(String bankAccountNumber, String bankCode) throws Exception {
        if (bankAccountNumber == null || bankAccountNumber.trim().isEmpty()) {
            throw new Exception("Số tài khoản ngân hàng không được để trống");
        }

        if (bankCode == null || bankCode.trim().isEmpty()) {
            throw new Exception("Mã ngân hàng không được để trống");
        }

        // Normalize bank code (uppercase, trim)
        String normalizedBankCode = bankCode.trim().toUpperCase();

        // Mock data: chỉ kiểm tra MB bank
        if ("MB".equals(normalizedBankCode) || "MBB".equals(normalizedBankCode)) {
            String normalizedAccountNumber = bankAccountNumber.trim();
            
            if ("123".equals(normalizedAccountNumber)) {
                logger.info("Mock verification: STK 123 + MB -> NGUYEN TRAN THANH DUY");
                return "NGUYEN TRAN THANH DUY";
            } else if ("321".equals(normalizedAccountNumber)) {
                logger.info("Mock verification: STK 321 + MB -> LE HUYNH DUC");
                return "LE HUYNH DUC";
            }
        }

        // Trường hợp khác: không tìm thấy hoặc không khớp
        logger.warn("Mock verification failed: STK {} + Bank {} -> không tìm thấy hoặc không khớp", 
                    bankAccountNumber, bankCode);
        return null;
    }

    /**
     * Chuẩn hóa tên để so sánh (bỏ dấu, chuyển thành chữ hoa)
     */
    public static String normalizeName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return "";
        }
        
        // Bỏ dấu tiếng Việt
        String normalized = Normalizer.normalize(name.trim(), Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        normalized = pattern.matcher(normalized).replaceAll("");
        
        // Chuyển thành chữ hoa và loại bỏ khoảng trắng thừa
        normalized = normalized.toUpperCase().replaceAll("\\s+", " ").trim();
        normalized = normalized.replace("Đ", "D");
        return normalized;
    }

    /**
     * So sánh hai tên có khớp nhau không (sau khi chuẩn hóa)
     */
    public static boolean compareNames(String name1, String name2) {
        String normalized1 = normalizeName(name1);
        String normalized2 = normalizeName(name2);
        return normalized1.equals(normalized2);
    }
}

