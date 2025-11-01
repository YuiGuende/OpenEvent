package com.group02.openevent.exception;

/**
 * Custom Exception cho các lỗi liên quan đến nghiệp vụ Ví (Wallet)
 * Ví dụ: Số dư không đủ, Ví không tồn tại, v.v.
 */
public class WalletException extends RuntimeException {

    // Constructor cơ bản
    public WalletException() {
        super();
    }

    // Constructor với thông báo lỗi
    public WalletException(String message) {
        super(message);
    }

    // Constructor với thông báo lỗi và nguyên nhân gốc
    public WalletException(String message, Throwable cause) {
        super(message, cause);
    }

    // Constructor với nguyên nhân gốc
    public WalletException(Throwable cause) {
        super(cause);
    }
}