package com.risopay.paymentapi.instruction;

import com.risopay.paymentapi.persistence.InstructionEntity;
import com.risopay.paymentapi.workflow.BusinessFunctionCode;
import com.risopay.paymentapi.workflow.WorkflowCode;
import com.risopay.paymentapi.workflow.WorkflowState;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class InstructionServiceTest {

    @Autowired
    private InstructionService instructionService;

    @Test
    void recordsReceivedInboundPaymentInstruction() {
        InstructionEntity instruction = instructionService
                .recordReceivedPaymentInstruction(
                        "pain.001.001.03",
                        "SOURCE-MSG-002",
                        2048L
                );

        assertTrue(instruction.getInstructionId()
                .matches("RISOPMTIINST\\d{10}"));
        assertEquals("SOURCE-MSG-002", instruction.getSourceMessageId());
        assertEquals(InstructionStatus.RECEIVED, instruction.getStatus());
        assertEquals(WorkflowCode.IN_PAIN_001, instruction.getWorkflowCode());
        assertEquals(WorkflowState.RUNNING, instruction.getWorkflowState());
        assertEquals(BusinessFunctionCode.TRANSACTION_PROCESSING,
                instruction.getCurrentBusinessFunction());
    }
}
