package com.risopay.paymentapi.instruction;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class InstructionIdGenerator {

    private static final int INSTRUCTION_ID_LENGTH = 22;

    private final SecureRandom secureRandom = new SecureRandom();

    public String newInstructionId(
            InstructionType instructionType,
            InstructionDirection direction
    ) {
        String prefix = "RISO" + typeCode(instructionType)
                + directionCode(direction) + "INST";
        int numericLength = INSTRUCTION_ID_LENGTH - prefix.length();

        StringBuilder instructionId = new StringBuilder(prefix);
        for (int index = 0; index < numericLength; index++) {
            instructionId.append(secureRandom.nextInt(10));
        }

        return instructionId.toString();
    }

    private String typeCode(InstructionType instructionType) {
        return switch (instructionType) {
            case PAYMENT -> "PMT";
            case DOCUMENT -> "DCN";
            case STATEMENT -> "STMT";
            case CAMT -> "CAMT";
            case FLAT_FILE -> "FLAT";
        };
    }

    private String directionCode(InstructionDirection direction) {
        return switch (direction) {
            case IN -> "I";
            case OUT -> "O";
        };
    }
}
