package com.risopay.paymentapi.persistence;

import com.risopay.paymentapi.instruction.InstructionDirection;
import com.risopay.paymentapi.instruction.InstructionSourceFormat;
import com.risopay.paymentapi.instruction.InstructionStatus;
import com.risopay.paymentapi.instruction.InstructionType;
import com.risopay.paymentapi.workflow.BusinessFunctionCode;
import com.risopay.paymentapi.workflow.WorkflowCode;
import com.risopay.paymentapi.workflow.WorkflowState;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "instruction")
public class InstructionEntity {

    @Id
    private UUID id;

    @Column(name = "instruction_id", nullable = false, unique = true, length = 22)
    private String instructionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 3)
    private InstructionDirection direction;

    @Enumerated(EnumType.STRING)
    @Column(name = "instruction_type", nullable = false, length = 32)
    private InstructionType instructionType;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_format", nullable = false, length = 32)
    private InstructionSourceFormat sourceFormat;

    @Column(name = "message_type", length = 64)
    private String messageType;

    @Column(name = "source_message_id", length = 64)
    private String sourceMessageId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 64)
    private InstructionStatus status;

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

    @Column(name = "source_instruction_id")
    private UUID sourceInstructionId;

    @Column(name = "file_reference", length = 1024)
    private String fileReference;

    @Column(name = "file_checksum", length = 128)
    private String fileChecksum;

    @Column(name = "file_size_bytes")
    private Long fileSizeBytes;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected InstructionEntity() {
    }

    public InstructionEntity(
            UUID id,
            String instructionId,
            InstructionDirection direction,
            InstructionType instructionType,
            InstructionSourceFormat sourceFormat,
            String messageType,
            String sourceMessageId,
            InstructionStatus status,
            WorkflowCode workflowCode,
            WorkflowState workflowState,
            BusinessFunctionCode currentBusinessFunction,
            UUID correlationId,
            UUID sourceInstructionId,
            String fileReference,
            String fileChecksum,
            Long fileSizeBytes,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = id;
        this.instructionId = instructionId;
        this.direction = direction;
        this.instructionType = instructionType;
        this.sourceFormat = sourceFormat;
        this.messageType = messageType;
        this.sourceMessageId = sourceMessageId;
        this.status = status;
        this.workflowCode = workflowCode;
        this.workflowState = workflowState;
        this.currentBusinessFunction = currentBusinessFunction;
        this.correlationId = correlationId;
        this.sourceInstructionId = sourceInstructionId;
        this.fileReference = fileReference;
        this.fileChecksum = fileChecksum;
        this.fileSizeBytes = fileSizeBytes;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() { return id; }
    public String getInstructionId() { return instructionId; }
    public InstructionDirection getDirection() { return direction; }
    public InstructionType getInstructionType() { return instructionType; }
    public InstructionSourceFormat getSourceFormat() { return sourceFormat; }
    public String getMessageType() { return messageType; }
    public String getSourceMessageId() { return sourceMessageId; }
    public InstructionStatus getStatus() { return status; }
    public WorkflowCode getWorkflowCode() { return workflowCode; }
    public WorkflowState getWorkflowState() { return workflowState; }
    public BusinessFunctionCode getCurrentBusinessFunction() { return currentBusinessFunction; }
    public UUID getCorrelationId() { return correlationId; }
    public UUID getSourceInstructionId() { return sourceInstructionId; }
    public String getFileReference() { return fileReference; }
    public String getFileChecksum() { return fileChecksum; }
    public Long getFileSizeBytes() { return fileSizeBytes; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
