package com.risopay.paymentapi.service;

import com.risopay.paymentapi.model.PaymentTransaction;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaymentBatchGrouperTest {

    private final PaymentBatchGrouper grouper =
            new PaymentBatchGrouper(new IdGenerator());

    @Test
    void incrementallyGroupsTransactionsInPaymentInformationOrder() {

        PaymentBatchGrouper.IncrementalBatchCollector collector =
                grouper.newIncrementalCollector();

        collector.accept(transaction("PMT-INF-ONE", "E2E-1", "100.00"));
        collector.accept(transaction("PMT-INF-TWO", "E2E-2", "200.00"));
        collector.accept(transaction("PMT-INF-ONE", "E2E-3", "300.00"));

        var batches = collector.toBatches();

        assertEquals(3, collector.transactions().size());
        assertEquals(2, batches.size());
        assertEquals("PMT-INF-ONE", batches.get(0).paymentInfoId());
        assertEquals(2, batches.get(0).transactions().size());
        assertEquals("E2E-3", batches.get(0).transactions().get(1).endToEndId());
        assertEquals("PMT-INF-TWO", batches.get(1).paymentInfoId());
        assertTrue(batches.get(0).batchId().startsWith("RISOBAT"));
    }

    @Test
    void doesNotCreateABatchForOneTransaction() {

        PaymentBatchGrouper.IncrementalBatchCollector collector =
                grouper.newIncrementalCollector();
        collector.accept(transaction("PMT-INF-ONE", "E2E-1", "100.00"));

        assertTrue(collector.toBatches().isEmpty());
    }

    private PaymentTransaction transaction(
            String paymentInfoId,
            String endToEndId,
            String amount
    ) {
        return new PaymentTransaction(
                paymentInfoId,
                endToEndId,
                new BigDecimal(amount),
                "EUR"
        );
    }
}
