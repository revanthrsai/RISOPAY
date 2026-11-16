package com.risopay.paymentapi.persistence;

import com.risopay.paymentapi.instruction.InstructionService;
import com.risopay.paymentapi.model.PaymentTransaction;
import com.risopay.paymentapi.service.PaymentBatchGrouper;
import com.risopay.paymentapi.transaction.PaymentTransactionStatus;
import com.risopay.paymentapi.workflow.BusinessFunctionCode;
import com.risopay.paymentapi.workflow.WorkflowCode;
import com.risopay.paymentapi.workflow.WorkflowState;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class PaymentInstructionPersistenceService {

    private final InstructionService instructionService;
    private final PaymentBatchRepository paymentBatchRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;

    public PaymentInstructionPersistenceService(
            InstructionService instructionService,
            PaymentBatchRepository paymentBatchRepository,
            PaymentTransactionRepository paymentTransactionRepository
    ) {
        this.instructionService = instructionService;
        this.paymentBatchRepository = paymentBatchRepository;
        this.paymentTransactionRepository = paymentTransactionRepository;
    }

    @Transactional
    public InstructionEntity recordReceivedPaymentInstruction(
            String messageType,
            String sourceMessageId,
            long fileSizeBytes,
            List<PaymentBatchGrouper.ExtractedBatch> batches,
            Map<PaymentTransaction, String> transactionIds
    ) {
        InstructionEntity instruction = instructionService
                .recordReceivedPaymentInstruction(
                        messageType,
                        sourceMessageId,
                        fileSizeBytes
                );

        for (PaymentBatchGrouper.ExtractedBatch batch : batches) {
            Instant now = Instant.now();
            PaymentBatchEntity persistedBatch = paymentBatchRepository.save(
                    new PaymentBatchEntity(
                            UUID.randomUUID(),
                            instruction.getId(),
                            batch.batchId(),
                            batch.paymentInfoId(),
                            batch.transactions().size(),
                            controlSum(batch.transactions()),
                            now,
                            now
                    )
            );

            for (PaymentTransaction transaction : batch.transactions()) {
                String transactionId = transactionIds.get(transaction);
                if (transactionId == null) {
                    throw new IllegalStateException(
                            "No RISOPAY transaction ID was generated for "
                                    + transaction.endToEndId()
                    );
                }

                paymentTransactionRepository.save(new PaymentTransactionEntity(
                        UUID.randomUUID(),
                        instruction.getId(),
                        persistedBatch.getId(),
                        transactionId,
                        transaction.endToEndId(),
                        transaction.amount(),
                        transaction.currency(),
                        PaymentTransactionStatus.PROCESSING,
                        WorkflowCode.PAYMENT_TRANSACTION,
                        WorkflowState.RUNNING,
                        BusinessFunctionCode.BUSINESS_VALIDATION,
                        instruction.getCorrelationId(),
                        now,
                        now
                ));
            }
        }

        return instruction;
    }

    private BigDecimal controlSum(List<PaymentTransaction> transactions) {
        return transactions.stream()
                .map(PaymentTransaction::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
