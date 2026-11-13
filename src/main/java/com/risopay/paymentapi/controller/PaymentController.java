
package com.risopay.paymentapi.controller;

import com.risopay.paymentapi.dto.PaymentInstructionResponse;
import com.risopay.paymentapi.dto.TransactionResponse;
import com.risopay.paymentapi.parser.PaymentDataExtractor;
import com.risopay.paymentapi.service.PaymentBatchGrouper;
import com.risopay.paymentapi.parser.XmlMessageTypeDetector;
import com.risopay.paymentapi.parser.XsdValidator;
import com.risopay.paymentapi.registry.MessageDefinitionRegistry;
import com.risopay.paymentapi.dto.BatchResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import org.xml.sax.SAXException;

import java.util.List;
import com.risopay.paymentapi.parser.PaymentDataExtractor.ExtractedPayment;
import com.risopay.paymentapi.service.IdGenerator;

@RestController
@RequestMapping("/api/v1")
public class PaymentController {

    private final XmlMessageTypeDetector xmlMessageTypeDetector;
    private final MessageDefinitionRegistry messageDefinitionRegistry;
    private final XsdValidator xsdValidator;
    private final PaymentDataExtractor paymentDataExtractor;
    private final PaymentBatchGrouper paymentBatchGrouper;
    private final IdGenerator idGenerator;


    public PaymentController(
            XmlMessageTypeDetector xmlMessageTypeDetector,
            MessageDefinitionRegistry messageDefinitionRegistry,
            XsdValidator xsdValidator,
            PaymentDataExtractor paymentDataExtractor,
            PaymentBatchGrouper paymentBatchGrouper,
            IdGenerator idGenerator) {

        this.xmlMessageTypeDetector = xmlMessageTypeDetector;
        this.messageDefinitionRegistry = messageDefinitionRegistry;
        this.xsdValidator = xsdValidator;
        this.paymentDataExtractor = paymentDataExtractor;
        this.paymentBatchGrouper = paymentBatchGrouper;
        this.idGenerator = idGenerator;
    }

    @PostMapping(
            value = "/payment-instructions",
            consumes = "application/xml",
            produces = "application/json"
    )
    public PaymentInstructionResponse submitPayment(
            @RequestBody String xmlPayload) throws Exception {

        // 1. Detect the incoming XML namespace
        String namespace =
                xmlMessageTypeDetector.detectMessageType(xmlPayload);

        // 2. Find the supported message definition
        var definition = messageDefinitionRegistry
                .findByNamespace(namespace)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Unsupported ISO 20022 message type"
                ));

        // 3. Validate XML against the registered XSD
        try {
            xsdValidator.validate(
                    xmlPayload,
                    definition.xsdResourcePath()
            );
        } catch (SAXException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "XML validation failed: " + exception.getMessage()
            );
        }

        // 4. Extract actual payment information
        ExtractedPayment extractedPayment;

        try {
            extractedPayment = paymentDataExtractor.extract(
                    xmlPayload,
                    namespace
            );
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    exception.getMessage()
            );
        }

        // 5. Generate RISOPAY transaction IDs
        List<TransactionResponse> transactions =
                extractedPayment.transactions()
                        .stream()
                        .map(transaction -> new TransactionResponse(
                                idGenerator.newTransactionId(),
                                transaction.paymentInfoId(),
                                transaction.endToEndId(),
                                transaction.amount(),
                                transaction.currency()
                        ))
                        .toList();

        // 6. Group transactions into logical batches
        var batches = paymentBatchGrouper.group(
                extractedPayment.transactions()
        );

        // 7. Convert grouped batches into API response objects
        List<BatchResponse> batchResponses = batches.stream()
                .map(batch -> {

                    List<TransactionResponse> batchTransactions =
                            transactions.stream()
                                    .filter(transaction ->
                                            transaction.getPaymentInfoId()
                                                    .equals(batch.paymentInfoId())
                                    )
                                    .toList();

                    return new BatchResponse(
                            batch.batchId(),
                            batch.paymentInfoId(),
                            batchTransactions.size(),
                            batchTransactions
                    );
                })
                .toList();

        // 8. Generate RISOPAY instruction ID
        String instructionId = idGenerator.newInstructionId();

        // 9. Return actual extracted information
        return new PaymentInstructionResponse(
                instructionId,
                definition.messageType(),
                extractedPayment.messageId(),
                transactions.size(),
                "RECEIVED",
                transactions,
                batchResponses
        );
    }
}
