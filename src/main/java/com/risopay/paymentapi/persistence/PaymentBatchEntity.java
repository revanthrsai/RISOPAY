package com.risopay.paymentapi.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payment_batch")
public class PaymentBatchEntity {

    @Id
    private UUID id;

    @Column(name = "instruction_id", nullable = false)
    private UUID instructionId;

    @Column(name = "batch_id", nullable = false, unique = true, length = 15)
    private String batchId;

    @Column(name = "payment_info_id", nullable = false, length = 35)
    private String paymentInfoId;

    @Column(name = "transaction_count", nullable = false)
    private int transactionCount;

    @Column(name = "control_sum", nullable = false, precision = 35, scale = 17)
    private BigDecimal controlSum;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected PaymentBatchEntity() {
    }

    public PaymentBatchEntity(
            UUID id,
            UUID instructionId,
            String batchId,
            String paymentInfoId,
            int transactionCount,
            BigDecimal controlSum,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = id;
        this.instructionId = instructionId;
        this.batchId = batchId;
        this.paymentInfoId = paymentInfoId;
        this.transactionCount = transactionCount;
        this.controlSum = controlSum;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() { return id; }
    public UUID getInstructionId() { return instructionId; }
    public String getBatchId() { return batchId; }
    public String getPaymentInfoId() { return paymentInfoId; }
    public int getTransactionCount() { return transactionCount; }
    public BigDecimal getControlSum() { return controlSum; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
