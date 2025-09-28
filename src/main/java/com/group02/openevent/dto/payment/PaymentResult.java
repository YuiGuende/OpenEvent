package com.group02.openevent.dto.payment;

import com.group02.openevent.model.payment.Payment;

public class PaymentResult {
    private boolean success;
    private String message;
    private Payment payment;
    private String redirectUrl;

    public PaymentResult() {}

    public PaymentResult(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public PaymentResult(boolean success, String message, Payment payment) {
        this.success = success;
        this.message = message;
        this.payment = payment;
    }

    public PaymentResult(boolean success, String message, Payment payment, String redirectUrl) {
        this.success = success;
        this.message = message;
        this.payment = payment;
        this.redirectUrl = redirectUrl;
    }

    // Getters and Setters
    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Payment getPayment() {
        return payment;
    }

    public void setPayment(Payment payment) {
        this.payment = payment;
    }

    public String getRedirectUrl() {
        return redirectUrl;
    }

    public void setRedirectUrl(String redirectUrl) {
        this.redirectUrl = redirectUrl;
    }
}
