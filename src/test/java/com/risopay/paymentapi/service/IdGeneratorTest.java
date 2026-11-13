package com.risopay.paymentapi.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IdGeneratorTest {

    private final IdGenerator idGenerator = new IdGenerator();

    @Test
    void instructionIdHasExpectedPrefixAndLength() {
        assertIdentifier(idGenerator.newInstructionId(), "RISOINS");
    }

    @Test
    void batchIdHasExpectedPrefixAndLength() {
        assertIdentifier(idGenerator.newBatchId(), "RISOBAT");
    }

    @Test
    void transactionIdHasExpectedPrefixAndLength() {
        assertIdentifier(idGenerator.newTransactionId(), "RISOTXN");
    }

    private void assertIdentifier(String id, String expectedPrefix) {
        assertTrue(id.startsWith(expectedPrefix));
        assertEquals(15, id.length());
        assertTrue(id.substring(expectedPrefix.length()).matches("[0-9]{8}"));
    }
}
