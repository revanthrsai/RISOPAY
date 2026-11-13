
package com.risopay.paymentapi.service;

import com.risopay.paymentapi.parser.PaymentDataExtractor.ExtractedTransaction;
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
            List<ExtractedTransaction> transactions
    ) {

        // Rule 1: A single transaction does not require a batch.
        if (transactions.size() <= 1) {
            return List.of();
        }

        // Group transactions using PmtInfId.
        Map<String, List<ExtractedTransaction>> groupedTransactions =
                new LinkedHashMap<>();

        for (ExtractedTransaction transaction : transactions) {

            groupedTransactions
                    .computeIfAbsent(
                            transaction.paymentInfoId(),
                            key -> new ArrayList<>()
                    )
                    .add(transaction);
        }

        // Create one batch for every unique PmtInfId.
        List<ExtractedBatch> batches = new ArrayList<>();

        for (Map.Entry<String, List<ExtractedTransaction>> entry
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

    public record ExtractedBatch(
            String batchId,
            String paymentInfoId,
            List<ExtractedTransaction> transactions
    ) {
    }
}
