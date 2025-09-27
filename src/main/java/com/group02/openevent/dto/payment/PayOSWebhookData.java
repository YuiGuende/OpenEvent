package com.group02.openevent.dto.payment;

public class PayOSWebhookData {
    private Integer code;
    private String desc;
    private Data data;
    private String signature;

    public PayOSWebhookData() {}

    // Getters and Setters
    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public Data getData() {
        return data;
    }

    public void setData(Data data) {
        this.data = data;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    // Inner class for data
    public static class Data {
        private Long orderCode;
        private Integer amount;
        private String description;
        private String accountNumber;
        private String reference;
        private Integer transactionDateTime;
        private String currency;
        private Long paymentLinkId;
        private String code;
        private String desc;
        private Long counterAccountBankId;
        private String counterAccountBankName;
        private String counterAccountName;
        private String counterAccountNumber;
        private String virtualAccountName;
        private String virtualAccountNumber;

        public Data() {}

        public Long getOrderCode() {
            return orderCode;
        }

        public void setOrderCode(Long orderCode) {
            this.orderCode = orderCode;
        }

        public Integer getAmount() {
            return amount;
        }

        public void setAmount(Integer amount) {
            this.amount = amount;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getAccountNumber() {
            return accountNumber;
        }

        public void setAccountNumber(String accountNumber) {
            this.accountNumber = accountNumber;
        }

        public String getReference() {
            return reference;
        }

        public void setReference(String reference) {
            this.reference = reference;
        }

        public Integer getTransactionDateTime() {
            return transactionDateTime;
        }

        public void setTransactionDateTime(Integer transactionDateTime) {
            this.transactionDateTime = transactionDateTime;
        }

        public String getCurrency() {
            return currency;
        }

        public void setCurrency(String currency) {
            this.currency = currency;
        }

        public Long getPaymentLinkId() {
            return paymentLinkId;
        }

        public void setPaymentLinkId(Long paymentLinkId) {
            this.paymentLinkId = paymentLinkId;
        }

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public String getDesc() {
            return desc;
        }

        public void setDesc(String desc) {
            this.desc = desc;
        }

        public Long getCounterAccountBankId() {
            return counterAccountBankId;
        }

        public void setCounterAccountBankId(Long counterAccountBankId) {
            this.counterAccountBankId = counterAccountBankId;
        }

        public String getCounterAccountBankName() {
            return counterAccountBankName;
        }

        public void setCounterAccountBankName(String counterAccountBankName) {
            this.counterAccountBankName = counterAccountBankName;
        }

        public String getCounterAccountName() {
            return counterAccountName;
        }

        public void setCounterAccountName(String counterAccountName) {
            this.counterAccountName = counterAccountName;
        }

        public String getCounterAccountNumber() {
            return counterAccountNumber;
        }

        public void setCounterAccountNumber(String counterAccountNumber) {
            this.counterAccountNumber = counterAccountNumber;
        }

        public String getVirtualAccountName() {
            return virtualAccountName;
        }

        public void setVirtualAccountName(String virtualAccountName) {
            this.virtualAccountName = virtualAccountName;
        }

        public String getVirtualAccountNumber() {
            return virtualAccountNumber;
        }

        public void setVirtualAccountNumber(String virtualAccountNumber) {
            this.virtualAccountNumber = virtualAccountNumber;
        }
    }
}
