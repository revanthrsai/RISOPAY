package com.risopay.paymentapi.dto;

import java.util.List;

public class BatchResponse {

    private String batchId;
    private String paymentInfoId;
    private int transactionCount;
    private List<TransactionResponse> transactions;

    public BatchResponse(
            String batchId,
            String paymentInfoId,
            int transactionCount,
            List<TransactionResponse> transactions
    ) {
        this.batchId = batchId;
        this.paymentInfoId = paymentInfoId;
        this.transactionCount = transactionCount;
        this.transactions = transactions;
    }

    public String getBatchId() {
        return batchId;
    }

    public String getPaymentInfoId() {
        return paymentInfoId;
    }

    public int getTransactionCount() {
        return transactionCount;
    }

    public List<TransactionResponse> getTransactions() {
        return transactions;
    }
}