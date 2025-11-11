package com.group02.openevent.service;

/**
 * Service để kiểm tra thông tin chủ tài khoản ngân hàng
 */
public interface BankVerificationService {
    /**
     * Kiểm tra tên chủ tài khoản ngân hàng có khớp với STK và mã ngân hàng không
     * @param bankAccountNumber Số tài khoản ngân hàng
     * @param bankCode Mã ngân hàng
     * @return Tên chủ tài khoản nếu hợp lệ, null nếu không hợp lệ hoặc không tìm thấy
     * @throws Exception Nếu có lỗi xảy ra khi kiểm tra
     */
    String verifyBankAccount(String bankAccountNumber, String bankCode) throws Exception;
}

