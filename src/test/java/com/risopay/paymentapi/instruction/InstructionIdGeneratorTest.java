package com.risopay.paymentapi.instruction;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InstructionIdGeneratorTest {

    private final InstructionIdGenerator generator = new InstructionIdGenerator();

    @Test
    void createsFixedLengthInboundPaymentInstructionId() {
        String instructionId = generator.newInstructionId(
                InstructionType.PAYMENT,
                InstructionDirection.IN
        );

        assertEquals(22, instructionId.length());
        assertTrue(instructionId.matches("RISOPMTIINST\\d{10}"));
    }

    @Test
    void createsFixedLengthOutboundStatementInstructionId() {
        String instructionId = generator.newInstructionId(
                InstructionType.STATEMENT,
                InstructionDirection.OUT
        );

        assertEquals(22, instructionId.length());
        assertTrue(instructionId.matches("RISOSTMTOINST\\d{9}"));
    }
}
