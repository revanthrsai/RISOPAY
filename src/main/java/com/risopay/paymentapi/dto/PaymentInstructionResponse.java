
package com.risopay.paymentapi.dto;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class PaymentInstructionResponse {

    private String instructionId;
    private String messageType;
    private String messageId;
    private int transactionCount;
    private String status;
    private List<TransactionResponse> transactions;
    private List<BatchResponse> batches;

    public PaymentInstructionResponse(
            String instructionId,
            String messageType,
            String messageId,
            int transactionCount,
            String status,
            List<TransactionResponse> transactions,
            List<BatchResponse> batches) {

        this.instructionId = instructionId;
        this.messageType = messageType;
        this.messageId = messageId;
        this.transactionCount = transactionCount;
        this.status = status;
        this.transactions = batches.isEmpty() ? transactions : null;
        this.batches = batches;
    }

    public String getInstructionId() {
        return instructionId;
    }

    public String getMessageType() {
        return messageType;
    }

    public String getMessageId() {
        return messageId;
    }

    public int getTransactionCount() {
        return transactionCount;
    }

    public String getStatus() {
        return status;
    }

    public List<TransactionResponse> getTransactions() {
        return transactions;
    }

    public List<BatchResponse> getBatches() {
        return batches;
    }
}
