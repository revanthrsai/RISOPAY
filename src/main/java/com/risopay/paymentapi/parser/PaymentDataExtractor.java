package com.risopay.paymentapi.parser;

import com.risopay.paymentapi.model.PaymentTransaction;
import com.risopay.paymentapi.processing.InMemoryPaymentTransactionSink;
import com.risopay.paymentapi.processing.PaymentTransactionSink;
import org.springframework.stereotype.Component;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Objects;

@Component
public class PaymentDataExtractor {

    public ExtractedPayment extract(
            InputStream xmlInput,
            String namespace
    ) throws XMLStreamException {

        InMemoryPaymentTransactionSink transactionSink =
                new InMemoryPaymentTransactionSink();

        ExtractionSummary summary = extract(
                xmlInput,
                namespace,
                transactionSink
        );

        return new ExtractedPayment(
                summary.messageId(),
                transactionSink.transactions()
        );
    }

    public ExtractionSummary extract(
            InputStream xmlInput,
            String namespace,
            PaymentTransactionSink transactionSink
    ) throws XMLStreamException {

        Objects.requireNonNull(transactionSink, "transactionSink");

        XMLStreamReader reader = createSecureReader(xmlInput);

        try {
            GroupHeader groupHeader = null;
            boolean customerCreditTransferInitiationFound = false;
            int actualGroupCount = 0;
            BigDecimal actualGroupControlSum = BigDecimal.ZERO;

            while (reader.hasNext()) {
                int event = reader.next();

                if (event != XMLStreamConstants.START_ELEMENT
                        || !namespace.equals(reader.getNamespaceURI())) {
                    continue;
                }

                String elementName = reader.getLocalName();

                if ("CstmrCdtTrfInitn".equals(elementName)) {
                    customerCreditTransferInitiationFound = true;
                } else if ("GrpHdr".equals(elementName)) {
                    groupHeader = readGroupHeader(reader, namespace);
                } else if ("PmtInf".equals(elementName)) {
                    PaymentInformationTotals paymentInformation =
                            readPaymentInformation(
                                    reader,
                                    namespace,
                                    transactionSink
                            );

                    actualGroupCount += paymentInformation.transactionCount();
                    actualGroupControlSum = actualGroupControlSum.add(
                            paymentInformation.controlSum()
                    );
                }
            }

            if (!customerCreditTransferInitiationFound) {
                throw missingElement("CstmrCdtTrfInitn");
            }

            if (groupHeader == null) {
                throw missingElement("GrpHdr");
            }

            if (groupHeader.declaredTransactionCount() != actualGroupCount) {
                throw new IllegalArgumentException(
                        "Transaction count mismatch. Declared: "
                                + groupHeader.declaredTransactionCount()
                                + ", Actual: "
                                + actualGroupCount
                );
            }

            if (groupHeader.declaredControlSum() != null
                    && groupHeader.declaredControlSum().compareTo(
                    actualGroupControlSum) != 0) {

                throw new IllegalArgumentException(
                        "Group header control sum mismatch. Declared: "
                                + groupHeader.declaredControlSum()
                                + ", Actual: "
                                + actualGroupControlSum
                );
            }

            return new ExtractionSummary(
                    groupHeader.messageId(),
                    actualGroupCount
            );
        } finally {
            reader.close();
        }
    }

    private XMLStreamReader createSecureReader(
            InputStream xmlInput
    ) throws XMLStreamException {

        XMLInputFactory factory = XMLInputFactory.newFactory();
        factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        factory.setProperty(
                XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES,
                false
        );
        factory.setXMLResolver((publicId, systemId, baseUri, name) -> {
            throw new XMLStreamException(
                    "External XML entity resolution is disabled."
            );
        });

        return factory.createXMLStreamReader(xmlInput);
    }

    private GroupHeader readGroupHeader(
            XMLStreamReader reader,
            String namespace
    ) throws XMLStreamException {

        String messageId = null;
        Integer declaredTransactionCount = null;
        BigDecimal declaredControlSum = null;

        while (reader.hasNext()) {
            int event = reader.next();

            if (isEndElement(reader, event, namespace, "GrpHdr")) {
                break;
            }

            if (!isStartElement(reader, event, namespace)) {
                continue;
            }

            switch (reader.getLocalName()) {
                case "MsgId" -> messageId = readRequiredText(reader, "MsgId");
                case "NbOfTxs" -> declaredTransactionCount = Integer.parseInt(
                        readRequiredText(reader, "NbOfTxs")
                );
                case "CtrlSum" -> declaredControlSum = new BigDecimal(
                        readRequiredText(reader, "CtrlSum")
                );
                default -> {
                    // The XSD validator has already checked other elements.
                }
            }
        }

        if (messageId == null) {
            throw missingElement("MsgId");
        }

        return new GroupHeader(
                messageId,
                declaredTransactionCount,
                declaredControlSum
        );
    }

    private PaymentInformationTotals readPaymentInformation(
            XMLStreamReader reader,
            String namespace,
            PaymentTransactionSink transactionSink
    ) throws XMLStreamException {

        String paymentInfoId = null;
        Integer declaredTransactionCount = null;
        BigDecimal declaredControlSum = null;
        int actualTransactionCount = 0;
        BigDecimal actualControlSum = BigDecimal.ZERO;

        while (reader.hasNext()) {
            int event = reader.next();

            if (isEndElement(reader, event, namespace, "PmtInf")) {
                break;
            }

            if (!isStartElement(reader, event, namespace)) {
                continue;
            }

            switch (reader.getLocalName()) {
                case "PmtInfId" -> paymentInfoId = readRequiredText(
                        reader,
                        "PmtInfId"
                );
                case "NbOfTxs" -> declaredTransactionCount = Integer.parseInt(
                        readRequiredText(reader, "NbOfTxs")
                );
                case "CtrlSum" -> declaredControlSum = new BigDecimal(
                        readRequiredText(reader, "CtrlSum")
                );
                case "CdtTrfTxInf" -> {
                    PaymentTransaction transaction = readTransaction(
                            reader,
                            namespace,
                            paymentInfoId
                    );

                    transactionSink.accept(transaction);
                    actualTransactionCount++;
                    actualControlSum = actualControlSum.add(
                            transaction.amount()
                    );
                }
                default -> {
                    // The XSD validator has already checked other elements.
                }
            }
        }

        if (paymentInfoId == null) {
            throw missingElement("PmtInfId");
        }

        if (declaredTransactionCount != null
                && declaredTransactionCount != actualTransactionCount) {
            throw new IllegalArgumentException(
                    "PmtInf transaction count mismatch for "
                            + paymentInfoId
                            + ". Declared: "
                            + declaredTransactionCount
                            + ", Actual: "
                            + actualTransactionCount
            );
        }

        if (declaredControlSum != null
                && declaredControlSum.compareTo(actualControlSum) != 0) {

            throw new IllegalArgumentException(
                    "PmtInf control sum mismatch for "
                            + paymentInfoId
                            + ". Declared: "
                            + declaredControlSum
                            + ", Actual: "
                            + actualControlSum
            );
        }

        return new PaymentInformationTotals(
                actualTransactionCount,
                actualControlSum
        );
    }

    private PaymentTransaction readTransaction(
            XMLStreamReader reader,
            String namespace,
            String paymentInfoId
    ) throws XMLStreamException {

        boolean paymentIdFound = false;
        boolean amountFound = false;
        boolean instructedAmountFound = false;
        String endToEndId = null;
        BigDecimal amount = null;
        String currency = null;
        Deque<String> elementPath = new ArrayDeque<>();
        elementPath.addLast("CdtTrfTxInf");

        while (reader.hasNext()) {
            int event = reader.next();

            if (isEndElement(reader, event, namespace, "CdtTrfTxInf")) {
                break;
            }

            if (event == XMLStreamConstants.START_ELEMENT) {
                String parentElement = elementPath.peekLast();
                String elementName = reader.getLocalName();
                elementPath.addLast(elementName);

                if (namespace.equals(reader.getNamespaceURI())
                        && "PmtId".equals(elementName)) {
                    paymentIdFound = true;
                } else if (namespace.equals(reader.getNamespaceURI())
                        && "EndToEndId".equals(elementName)
                        && "PmtId".equals(parentElement)) {
                    endToEndId = readRequiredText(reader, "EndToEndId");
                    elementPath.removeLast();
                } else if (namespace.equals(reader.getNamespaceURI())
                        && "Amt".equals(elementName)) {
                    amountFound = true;
                } else if (namespace.equals(reader.getNamespaceURI())
                        && "InstdAmt".equals(elementName)
                        && "Amt".equals(parentElement)) {
                    instructedAmountFound = true;
                    currency = reader.getAttributeValue(null, "Ccy");
                    amount = new BigDecimal(
                            readRequiredText(reader, "InstdAmt")
                    );
                    elementPath.removeLast();
                }
            } else if (event == XMLStreamConstants.END_ELEMENT
                    && !elementPath.isEmpty()
                    && reader.getLocalName().equals(elementPath.peekLast())) {
                elementPath.removeLast();
            }
        }

        if (!paymentIdFound) {
            throw missingElement("PmtId");
        }

        if (endToEndId == null) {
            throw missingElement("EndToEndId");
        }

        if (!amountFound) {
            throw missingElement("Amt");
        }

        if (!instructedAmountFound) {
            throw missingElement("InstdAmt");
        }

        if (currency == null || currency.isBlank()) {
            throw new IllegalArgumentException(
                    "Missing currency in instructed amount"
            );
        }

        return new PaymentTransaction(
                paymentInfoId,
                endToEndId,
                amount,
                currency
        );
    }

    private boolean isStartElement(
            XMLStreamReader reader,
            int event,
            String namespace
    ) {
        return event == XMLStreamConstants.START_ELEMENT
                && namespace.equals(reader.getNamespaceURI());
    }

    private boolean isEndElement(
            XMLStreamReader reader,
            int event,
            String namespace,
            String elementName
    ) {
        return event == XMLStreamConstants.END_ELEMENT
                && namespace.equals(reader.getNamespaceURI())
                && elementName.equals(reader.getLocalName());
    }

    private String readRequiredText(
            XMLStreamReader reader,
            String elementName
    ) throws XMLStreamException {

        String value = reader.getElementText().trim();

        if (value.isBlank()) {
            throw new IllegalArgumentException(
                    "Empty XML element: " + elementName
            );
        }

        return value;
    }

    private IllegalArgumentException missingElement(String elementName) {
        return new IllegalArgumentException(
                "Missing required XML element: " + elementName
        );
    }

    public record ExtractedPayment(
            String messageId,
            List<PaymentTransaction> transactions
    ) {
    }

    public record ExtractionSummary(
            String messageId,
            int transactionCount
    ) {
    }

    private record GroupHeader(
            String messageId,
            Integer declaredTransactionCount,
            BigDecimal declaredControlSum
    ) {
    }

    private record PaymentInformationTotals(
            int transactionCount,
            BigDecimal controlSum
    ) {
    }
}
