package com.risopay.paymentapi.instruction;

import com.risopay.paymentapi.persistence.InstructionEntity;
import com.risopay.paymentapi.persistence.InstructionRepository;
import com.risopay.paymentapi.workflow.BusinessFunctionCode;
import com.risopay.paymentapi.workflow.WorkflowCode;
import com.risopay.paymentapi.workflow.WorkflowState;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class InstructionService {

    private static final int MAX_ID_GENERATION_ATTEMPTS = 5;

    private final InstructionRepository instructionRepository;
    private final InstructionIdGenerator instructionIdGenerator;

    public InstructionService(
            InstructionRepository instructionRepository,
            InstructionIdGenerator instructionIdGenerator
    ) {
        this.instructionRepository = instructionRepository;
        this.instructionIdGenerator = instructionIdGenerator;
    }

    @Transactional
    public InstructionEntity recordReceivedPaymentInstruction(
            String messageType,
            String sourceMessageId,
            long fileSizeBytes
    ) {
        for (int attempt = 0; attempt < MAX_ID_GENERATION_ATTEMPTS; attempt++) {
            String instructionId = instructionIdGenerator.newInstructionId(
                    InstructionType.PAYMENT,
                    InstructionDirection.IN
            );

            if (instructionRepository.existsByInstructionId(instructionId)) {
                continue;
            }

            Instant now = Instant.now();
            return instructionRepository.save(new InstructionEntity(
                    UUID.randomUUID(),
                    instructionId,
                    InstructionDirection.IN,
                    InstructionType.PAYMENT,
                    InstructionSourceFormat.ISO_20022_XML,
                    messageType,
                    sourceMessageId,
                    InstructionStatus.RECEIVED,
                    WorkflowCode.IN_PAIN_001,
                    WorkflowState.RUNNING,
                    BusinessFunctionCode.TRANSACTION_PROCESSING,
                    UUID.randomUUID(),
                    null,
                    null,
                    null,
                    fileSizeBytes,
                    now,
                    now
            ));
        }

        throw new IllegalStateException("Unable to generate a unique instruction ID.");
    }
}
