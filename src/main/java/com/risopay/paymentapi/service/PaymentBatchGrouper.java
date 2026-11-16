
package com.risopay.paymentapi.service;

import com.risopay.paymentapi.model.PaymentTransaction;
import com.risopay.paymentapi.processing.PaymentTransactionSink;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class PaymentBatchGrouper {

    private final IdGenerator idGenerator;

    public PaymentBatchGrouper(IdGenerator idGenerator) {
        this.idGenerator = idGenerator;
    }

    public List<ExtractedBatch> group(
            List<PaymentTransaction> transactions
    ) {

        IncrementalBatchCollector collector = newIncrementalCollector();
        transactions.forEach(collector::accept);
        return collector.toBatches();
    }

    public IncrementalBatchCollector newIncrementalCollector() {
        return new IncrementalBatchCollector();
    }

    public final class IncrementalBatchCollector
            implements PaymentTransactionSink {

        private final List<PaymentTransaction> transactions =
                new ArrayList<>();

        Map<String, List<PaymentTransaction>> groupedTransactions =
                new LinkedHashMap<>();

        @Override
        public void accept(PaymentTransaction transaction) {

            transactions.add(transaction);

            groupedTransactions
                    .computeIfAbsent(
                            transaction.paymentInfoId(),
                            key -> new ArrayList<>()
                    )
                    .add(transaction);
        }

        public List<PaymentTransaction> transactions() {
            return List.copyOf(transactions);
        }

        public List<ExtractedBatch> toBatches() {

            // Preserve the existing single-transaction rule.
            if (transactions.size() <= 1) {
                return List.of();
            }

            return createBatches();
        }

        public List<ExtractedBatch> toAllBatches() {
            return createBatches();
        }

        private List<ExtractedBatch> createBatches() {
            List<ExtractedBatch> batches = new ArrayList<>();

            for (Map.Entry<String, List<PaymentTransaction>> entry
                    : groupedTransactions.entrySet()) {

                String batchId = idGenerator.newBatchId();

                batches.add(
                        new ExtractedBatch(
                                batchId,
                                entry.getKey(),
                                entry.getValue()
                        )
                );
            }

            return batches;
        }
    }

    public record ExtractedBatch(
            String batchId,
            String paymentInfoId,
            List<PaymentTransaction> transactions
    ) {
    }
}
