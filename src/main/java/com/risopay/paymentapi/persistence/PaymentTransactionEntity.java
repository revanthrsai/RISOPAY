package com.risopay.paymentapi.persistence;

import com.risopay.paymentapi.transaction.PaymentTransactionStatus;
import com.risopay.paymentapi.workflow.BusinessFunctionCode;
import com.risopay.paymentapi.workflow.WorkflowCode;
import com.risopay.paymentapi.workflow.WorkflowState;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payment_transaction")
public class PaymentTransactionEntity {

    @Id
    private UUID id;

    @Column(name = "instruction_id", nullable = false)
    private UUID instructionId;

    @Column(name = "payment_batch_id", nullable = false)
    private UUID paymentBatchId;

    @Column(name = "transaction_id", nullable = false, unique = true, length = 15)
    private String transactionId;

    @Column(name = "end_to_end_id", nullable = false, length = 35)
    private String endToEndId;

    @Column(name = "instructed_amount", nullable = false, precision = 18, scale = 5)
    private BigDecimal instructedAmount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private PaymentTransactionStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "workflow_code", nullable = false, length = 64)
    private WorkflowCode workflowCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "workflow_state", nullable = false, length = 16)
    private WorkflowState workflowState;

    @Enumerated(EnumType.STRING)
    @Column(name = "current_business_function", nullable = false, length = 64)
    private BusinessFunctionCode currentBusinessFunction;

    @Column(name = "correlation_id", nullable = false)
    private UUID correlationId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected PaymentTransactionEntity() {
    }

    public PaymentTransactionEntity(
            UUID id,
            UUID instructionId,
            UUID paymentBatchId,
            String transactionId,
            String endToEndId,
            BigDecimal instructedAmount,
            String currency,
            PaymentTransactionStatus status,
            WorkflowCode workflowCode,
            WorkflowState workflowState,
            BusinessFunctionCode currentBusinessFunction,
            UUID correlationId,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = id;
        this.instructionId = instructionId;
        this.paymentBatchId = paymentBatchId;
        this.transactionId = transactionId;
        this.endToEndId = endToEndId;
        this.instructedAmount = instructedAmount;
        this.currency = currency;
        this.status = status;
        this.workflowCode = workflowCode;
        this.workflowState = workflowState;
        this.currentBusinessFunction = currentBusinessFunction;
        this.correlationId = correlationId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() { return id; }
    public UUID getInstructionId() { return instructionId; }
    public UUID getPaymentBatchId() { return paymentBatchId; }
    public String getTransactionId() { return transactionId; }
    public String getEndToEndId() { return endToEndId; }
    public BigDecimal getInstructedAmount() { return instructedAmount; }
    public String getCurrency() { return currency; }
    public PaymentTransactionStatus getStatus() { return status; }
    public WorkflowCode getWorkflowCode() { return workflowCode; }
    public WorkflowState getWorkflowState() { return workflowState; }
    public BusinessFunctionCode getCurrentBusinessFunction() { return currentBusinessFunction; }
    public UUID getCorrelationId() { return correlationId; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
