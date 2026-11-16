package com.risopay.paymentapi.persistence;

import com.risopay.paymentapi.instruction.InstructionDirection;
import com.risopay.paymentapi.instruction.InstructionSourceFormat;
import com.risopay.paymentapi.instruction.InstructionStatus;
import com.risopay.paymentapi.instruction.InstructionType;
import com.risopay.paymentapi.workflow.BusinessFunctionCode;
import com.risopay.paymentapi.workflow.WorkflowCode;
import com.risopay.paymentapi.workflow.WorkflowState;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class InstructionRepositoryTest {

    @Autowired
    private InstructionRepository instructionRepository;

    @Test
    void savesAndFindsAnInstructionByBusinessId() {
        Instant now = Instant.now();
        String instructionId = "RISOPMTIINST1234567890";

        instructionRepository.save(new InstructionEntity(
                UUID.randomUUID(),
                instructionId,
                InstructionDirection.IN,
                InstructionType.PAYMENT,
                InstructionSourceFormat.ISO_20022_XML,
                "pain.001.001.03",
                "SOURCE-MSG-001",
                InstructionStatus.RECEIVED,
                WorkflowCode.IN_PAIN_001,
                WorkflowState.RUNNING,
                BusinessFunctionCode.INSTRUCTION_INTAKE,
                UUID.randomUUID(),
                null,
                null,
                null,
                1024L,
                now,
                now
        ));

        InstructionEntity saved = instructionRepository
                .findByInstructionId(instructionId)
                .orElseThrow();

        assertEquals(InstructionDirection.IN, saved.getDirection());
        assertEquals(InstructionStatus.RECEIVED, saved.getStatus());
        assertEquals("SOURCE-MSG-001", saved.getSourceMessageId());
        assertEquals(WorkflowState.RUNNING, saved.getWorkflowState());
    }
}
