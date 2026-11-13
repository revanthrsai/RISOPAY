
package com.risopay.paymentapi.dto;

import java.math.BigDecimal;

public class TransactionResponse {

    private String transactionId;
    private String paymentInfoId;
    private String endToEndId;
    private BigDecimal amount;
    private String currency;

    public TransactionResponse(
            String transactionId,
            String paymentInfoId,
            String endToEndId,
            BigDecimal amount,
            String currency) {

        this.transactionId = transactionId;
        this.paymentInfoId = paymentInfoId;
        this.endToEndId = endToEndId;
        this.amount = amount;
        this.currency = currency;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public String getPaymentInfoId() {
        return paymentInfoId;
    }

    public String getEndToEndId() {
        return endToEndId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }
}
