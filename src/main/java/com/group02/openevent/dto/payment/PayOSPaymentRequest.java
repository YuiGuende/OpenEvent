package com.group02.openevent.dto.payment;

import java.util.List;

public class PayOSPaymentRequest {
    private int orderCode;
    private int amount;
    private String description;
    private List<PayOSItem> items;
    private String returnUrl;
    private String cancelUrl;
    private long expiredAt;
    private String signature;

    public PayOSPaymentRequest() {}

    public PayOSPaymentRequest(int orderCode, int amount, String description, List<PayOSItem> items, 
                              String returnUrl, String cancelUrl, long expiredAt, String signature) {
        this.orderCode = orderCode;
        this.amount = amount;
        this.description = description;
        this.items = items;
        this.returnUrl = returnUrl;
        this.cancelUrl = cancelUrl;
        this.expiredAt = expiredAt;
        this.signature = signature;
    }

    // Getters and Setters
    public int getOrderCode() {
        return orderCode;
    }

    public void setOrderCode(int orderCode) {
        this.orderCode = orderCode;
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<PayOSItem> getItems() {
        return items;
    }

    public void setItems(List<PayOSItem> items) {
        this.items = items;
    }

    public String getReturnUrl() {
        return returnUrl;
    }

    public void setReturnUrl(String returnUrl) {
        this.returnUrl = returnUrl;
    }

    public String getCancelUrl() {
        return cancelUrl;
    }

    public void setCancelUrl(String cancelUrl) {
        this.cancelUrl = cancelUrl;
    }

    public long getExpiredAt() {
        return expiredAt;
    }

    public void setExpiredAt(long expiredAt) {
        this.expiredAt = expiredAt;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    @Override
    public String toString() {
        return "PayOSPaymentRequest{" +
                "orderCode=" + orderCode +
                ", amount=" + amount +
                ", description='" + description + '\'' +
                ", items=" + items +
                ", returnUrl='" + returnUrl + '\'' +
                ", cancelUrl='" + cancelUrl + '\'' +
                ", expiredAt=" + expiredAt +
                ", signature='" + signature + '\'' +
                '}';
    }
}