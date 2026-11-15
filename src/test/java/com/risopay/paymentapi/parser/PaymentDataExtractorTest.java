package com.risopay.paymentapi.parser;

import com.risopay.paymentapi.processing.InMemoryPaymentTransactionSink;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PaymentDataExtractorTest {

    private static final String PAIN_001_NAMESPACE =
            "urn:iso:std:iso:20022:tech:xsd:pain.001.001.03";

    private final PaymentDataExtractor extractor =
            new PaymentDataExtractor();

    @Test
    void extractsOneTransactionFromStaxEvents() throws Exception {

        var payment = extractFixture("valid-single-transaction.xml");

        assertEquals("VALID-TEST-MESSAGE", payment.messageId());
        assertEquals(1, payment.transactions().size());

        var transaction = payment.transactions().getFirst();
        assertEquals("VALID-TEST-PMT-INF", transaction.paymentInfoId());
        assertEquals("VALID-TEST-E2E", transaction.endToEndId());
        assertEquals(new BigDecimal("100.00"), transaction.amount());
        assertEquals("EUR", transaction.currency());
    }

    @Test
    void extractsTransactionsAcrossMultiplePaymentInformationGroups()
            throws Exception {

        var payment = extractFixture("valid-multiple-payment-information.xml");

        assertEquals("MULTI-PMT-INF-TEST", payment.messageId());
        assertEquals(2, payment.transactions().size());
        assertEquals(
                "PMT-INF-ONE",
                payment.transactions().get(0).paymentInfoId()
        );
        assertEquals(
                "E2E-TWO",
                payment.transactions().get(1).endToEndId()
        );
        assertEquals(
                new BigDecimal("200.00"),
                payment.transactions().get(1).amount()
        );
    }

    @Test
    void emitsTransactionsToTheProvidedSink() throws Exception {

        InMemoryPaymentTransactionSink transactionSink =
                new InMemoryPaymentTransactionSink();
        ClassPathResource resource = new ClassPathResource(
                "fixtures/pain001/valid-single-transaction.xml"
        );

        PaymentDataExtractor.ExtractionSummary summary;

        try (InputStream input = resource.getInputStream()) {
            summary = extractor.extract(
                    input,
                    PAIN_001_NAMESPACE,
                    transactionSink
            );
        }

        assertEquals("VALID-TEST-MESSAGE", summary.messageId());
        assertEquals(1, summary.transactionCount());
        assertEquals(1, transactionSink.transactions().size());
        assertEquals(
                "VALID-TEST-E2E",
                transactionSink.transactions().getFirst().endToEndId()
        );
    }

    @Test
    void rejectsPaymentInformationTransactionCountMismatch()
            throws Exception {

        String invalidXml = validSingleTransactionXml().replace(
                "            <NbOfTxs>1</NbOfTxs>\n"
                        + "            <CtrlSum>100.00</CtrlSum>\n"
                        + "            <ReqdExctnDt>",
                "            <NbOfTxs>2</NbOfTxs>\n"
                        + "            <CtrlSum>100.00</CtrlSum>\n"
                        + "            <ReqdExctnDt>"
        );

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> extract(invalidXml)
        );

        assertEquals(
                "PmtInf transaction count mismatch for VALID-TEST-PMT-INF. "
                        + "Declared: 2, Actual: 1",
                exception.getMessage()
        );
    }

    @Test
    void rejectsPaymentInformationControlSumMismatch() throws Exception {

        String invalidXml = validSingleTransactionXml().replace(
                "            <CtrlSum>100.00</CtrlSum>\n"
                        + "            <ReqdExctnDt>",
                "            <CtrlSum>200.00</CtrlSum>\n"
                        + "            <ReqdExctnDt>"
        );

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> extract(invalidXml)
        );

        assertEquals(
                "PmtInf control sum mismatch for VALID-TEST-PMT-INF. "
                        + "Declared: 200.00, Actual: 100.00",
                exception.getMessage()
        );
    }

    @Test
    void rejectsMissingTransactionCurrency() throws Exception {

        String invalidXml = validSingleTransactionXml().replace(
                " Ccy=\"EUR\"",
                ""
        );

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> extract(invalidXml)
        );

        assertEquals(
                "Missing currency in instructed amount",
                exception.getMessage()
        );
    }

    private PaymentDataExtractor.ExtractedPayment extractFixture(
            String fixtureName
    ) throws Exception {

        ClassPathResource resource = new ClassPathResource(
                "fixtures/pain001/" + fixtureName
        );

        try (InputStream input = resource.getInputStream()) {
            return extractor.extract(input, PAIN_001_NAMESPACE);
        }
    }

    private PaymentDataExtractor.ExtractedPayment extract(String xml)
            throws Exception {

        return extractor.extract(
                new ByteArrayInputStream(
                        xml.getBytes(StandardCharsets.UTF_8)
                ),
                PAIN_001_NAMESPACE
        );
    }

    private String validSingleTransactionXml() throws Exception {
        ClassPathResource resource = new ClassPathResource(
                "fixtures/pain001/valid-single-transaction.xml"
        );

        try (InputStream input = resource.getInputStream()) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
