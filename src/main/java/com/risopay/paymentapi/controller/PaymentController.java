
package com.risopay.paymentapi.controller;

import com.risopay.paymentapi.dto.PaymentInstructionResponse;
import com.risopay.paymentapi.dto.TransactionResponse;
import com.risopay.paymentapi.input.ReplayableXmlInput;
import com.risopay.paymentapi.model.PaymentTransaction;
import com.risopay.paymentapi.parser.PaymentDataExtractor;
import com.risopay.paymentapi.parser.PaymentDataExtractor.ExtractionSummary;
import com.risopay.paymentapi.service.PaymentBatchGrouper;
import com.risopay.paymentapi.parser.XmlMessageTypeDetector;
import com.risopay.paymentapi.parser.XsdValidator;
import com.risopay.paymentapi.registry.MessageDefinitionRegistry;
import com.risopay.paymentapi.dto.BatchResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import jakarta.servlet.http.HttpServletRequest;

import org.xml.sax.SAXException;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.risopay.paymentapi.service.IdGenerator;
import com.risopay.paymentapi.instruction.InstructionService;
import com.risopay.paymentapi.persistence.PaymentInstructionPersistenceService;

@RestController
@RequestMapping("/api/v1")
public class PaymentController {

    private final XmlMessageTypeDetector xmlMessageTypeDetector;
    private final MessageDefinitionRegistry messageDefinitionRegistry;
    private final XsdValidator xsdValidator;
    private final PaymentDataExtractor paymentDataExtractor;
    private final PaymentBatchGrouper paymentBatchGrouper;
    private final IdGenerator idGenerator;
    private final InstructionService instructionService;
    private final PaymentInstructionPersistenceService paymentInstructionPersistenceService;


    public PaymentController(
            XmlMessageTypeDetector xmlMessageTypeDetector,
            MessageDefinitionRegistry messageDefinitionRegistry,
            XsdValidator xsdValidator,
            PaymentDataExtractor paymentDataExtractor,
            PaymentBatchGrouper paymentBatchGrouper,
            IdGenerator idGenerator,
            InstructionService instructionService,
            PaymentInstructionPersistenceService paymentInstructionPersistenceService) {

        this.xmlMessageTypeDetector = xmlMessageTypeDetector;
        this.messageDefinitionRegistry = messageDefinitionRegistry;
        this.xsdValidator = xsdValidator;
        this.paymentDataExtractor = paymentDataExtractor;
        this.paymentBatchGrouper = paymentBatchGrouper;
        this.idGenerator = idGenerator;
        this.instructionService = instructionService;
        this.paymentInstructionPersistenceService = paymentInstructionPersistenceService;
    }

    @PostMapping(
            value = "/payment-instructions",
            consumes = "application/xml",
            produces = "application/json"
    )
    public PaymentInstructionResponse submitPayment(
            HttpServletRequest request) throws Exception {

        ReplayableXmlInput xmlInput = (ReplayableXmlInput) request
                .getAttribute(ReplayableXmlInput.REQUEST_ATTRIBUTE);

        if (xmlInput == null) {
            throw new IllegalStateException(
                    "Replayable XML request input was not initialized."
            );
        }

        // 1. Detect the incoming XML namespace
        String namespace;

        try (InputStream inputStream = xmlInput.openStream()) {
            namespace = xmlMessageTypeDetector.detectMessageType(inputStream);
        }

        // 2. Find the supported message definition
        var definition = messageDefinitionRegistry
                .findByNamespace(namespace)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Unsupported ISO 20022 message type"
                ));

        // 3. Validate XML against the registered XSD
        try {
            try (InputStream inputStream = xmlInput.openStream()) {
                xsdValidator.validate(
                        inputStream,
                        definition.xsdResourcePath()
                );
            }
        } catch (SAXException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "XML validation failed: " + exception.getMessage()
            );
        }

        // 4. Extract actual payment information
        ExtractionSummary extractionSummary;
        PaymentBatchGrouper.IncrementalBatchCollector batchCollector =
                paymentBatchGrouper.newIncrementalCollector();

        try {
            try (InputStream inputStream = xmlInput.openStream()) {
                extractionSummary = paymentDataExtractor.extract(
                        inputStream,
                        namespace,
                        batchCollector
                );
            }
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    exception.getMessage()
            );
        }

        // 5. Generate RISOPAY transaction IDs
        List<TransactionResponse> transactions = new ArrayList<>();
        Map<String, List<TransactionResponse>> transactionsByPaymentInfo =
                new LinkedHashMap<>();
        Map<PaymentTransaction, String> transactionIds = new LinkedHashMap<>();

        for (PaymentTransaction transaction : batchCollector.transactions()) {
            TransactionResponse response = new TransactionResponse(
                    idGenerator.newTransactionId(),
                    transaction.paymentInfoId(),
                    transaction.endToEndId(),
                    transaction.amount(),
                    transaction.currency()
            );

            transactions.add(response);
            transactionIds.put(transaction, response.getTransactionId());
            transactionsByPaymentInfo
                    .computeIfAbsent(
                            transaction.paymentInfoId(),
                            key -> new ArrayList<>()
                    )
                    .add(response);
        }

        // 6. Group transactions into logical batches
        var allBatches = batchCollector.toAllBatches();
        var batches = extractionSummary.transactionCount() <= 1
                ? List.<PaymentBatchGrouper.ExtractedBatch>of()
                : allBatches;

        // 7. Convert grouped batches into API response objects
        List<BatchResponse> batchResponses = batches.stream()
                .map(batch -> {

                    List<TransactionResponse> batchTransactions =
                            transactionsByPaymentInfo.get(
                                    batch.paymentInfoId()
                            );

                    return new BatchResponse(
                            batch.batchId(),
                            batch.paymentInfoId(),
                            batchTransactions.size(),
                            batchTransactions
                    );
                })
                .toList();

        // 8. Persist the accepted inbound instruction and use its business ID.
        String instructionId = paymentInstructionPersistenceService
                .recordReceivedPaymentInstruction(
                        definition.messageType(),
                        extractionSummary.messageId(),
                        xmlInput.sizeInBytes(),
                        allBatches,
                        transactionIds
                )
                .getInstructionId();

        // 9. Return actual extracted information
        return new PaymentInstructionResponse(
                instructionId,
                definition.messageType(),
                extractionSummary.messageId(),
                extractionSummary.transactionCount(),
                "RECEIVED",
                transactions,
                batchResponses
        );
    }
}
