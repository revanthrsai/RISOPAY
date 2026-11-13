
package com.risopay.paymentapi.parser;

import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Component
public class PaymentDataExtractor {

    public ExtractedPayment extract(
            String xmlPayload,
            String namespace
    ) throws Exception {

        // 1. Parse the XML securely
        DocumentBuilderFactory factory =
                DocumentBuilderFactory.newInstance();

        factory.setNamespaceAware(true);

        factory.setFeature(
                "http://apache.org/xml/features/disallow-doctype-decl",
                true
        );

        factory.setFeature(
                "http://xml.org/sax/features/external-general-entities",
                false
        );

        factory.setFeature(
                "http://xml.org/sax/features/external-parameter-entities",
                false
        );

        factory.setAttribute(
                XMLConstants.ACCESS_EXTERNAL_DTD,
                ""
        );

        factory.setAttribute(
                XMLConstants.ACCESS_EXTERNAL_SCHEMA,
                ""
        );

        Document document = factory
                .newDocumentBuilder()
                .parse(new InputSource(new StringReader(xmlPayload)));

        // 2. Navigate to Customer Credit Transfer Initiation
        Element root = document.getDocumentElement();

        Element initiation = requiredChild(
                root, "CstmrCdtTrfInitn", namespace
        );

        // 3. Extract message ID
        Element groupHeader = requiredChild(
                initiation, "GrpHdr", namespace
        );

        String messageId = requiredText(
                groupHeader, "MsgId", namespace
        );

        // get total txn count
        String declaredGroupCountText = optionalText(
                groupHeader, "NbOfTxs", namespace
        );

        Integer declaredGroupCount =
                declaredGroupCountText == null
                        ? null
                        : Integer.parseInt(declaredGroupCountText);

        // get total control sum
        String declaredControlSumText = optionalText(
                groupHeader, "CtrlSum", namespace
        );

        BigDecimal declaredGroupControlSum =
                declaredControlSumText == null
                        ? null
                        : new BigDecimal(declaredControlSumText);

        // 4. Extract payment information and transactions
        List<ExtractedTransaction> transactions = new ArrayList<>();
        int actualGroupCount = 0;
        BigDecimal actualGroupControlSum = BigDecimal.ZERO;

        NodeList paymentInfos =
                initiation.getElementsByTagNameNS(namespace, "PmtInf");

        for (int i = 0; i < paymentInfos.getLength(); i++) {

            Element paymentInfo = (Element) paymentInfos.item(i);

            String paymentInfoId = requiredText(
                    paymentInfo, "PmtInfId", namespace
            );

            String declaredPmtInfoCountText = optionalText(
                    paymentInfo, "NbOfTxs", namespace
            );

            Integer declaredPmtInfoCount =
                    declaredPmtInfoCountText == null
                            ? null
                            : Integer.parseInt(declaredPmtInfoCountText);

            int actualPmtInfoCount = 0;

            String declaredPmtInfoControlSumText = optionalText(
                    paymentInfo, "CtrlSum", namespace
            );

            BigDecimal declaredPmtInfoControlSum =
                    declaredPmtInfoControlSumText == null
                            ? null
                            : new BigDecimal(declaredPmtInfoControlSumText);

            BigDecimal actualPmtInfoControlSum = BigDecimal.ZERO;

            NodeList transactionNodes =
                    paymentInfo.getElementsByTagNameNS(
                            namespace, "CdtTrfTxInf"
                    );

            for (int j = 0; j < transactionNodes.getLength(); j++) {

                Element transaction =
                        (Element) transactionNodes.item(j);

                // Extract EndToEndId
                Element paymentId = requiredChild(
                        transaction, "PmtId", namespace
                );

                String endToEndId = requiredText(
                        paymentId, "EndToEndId", namespace
                );

                // Extract amount and currency
                Element amount = requiredChild(
                        transaction, "Amt", namespace
                );

                Element instructedAmount = requiredChild(
                        amount, "InstdAmt", namespace
                );

                BigDecimal amountValue =
                        new BigDecimal(instructedAmount.getTextContent().trim());

                actualGroupControlSum =
                        actualGroupControlSum.add(amountValue);

                actualPmtInfoControlSum =
                        actualPmtInfoControlSum.add(amountValue);

                String currency =
                        instructedAmount.getAttribute("Ccy");

                if (currency.isBlank()) {
                    throw new IllegalArgumentException(
                            "Missing currency in instructed amount"
                    );
                }

                transactions.add(
                        new ExtractedTransaction(
                                paymentInfoId,
                                endToEndId,
                                amountValue,
                                currency
                        )
                );
                actualGroupCount++;
                actualPmtInfoCount++;

            }
            if (declaredPmtInfoCount != null
                    && declaredPmtInfoCount != actualPmtInfoCount) {
                throw new IllegalArgumentException(
                        "PmtInf transaction count mismatch for "
                                + paymentInfoId
                                + ". Declared: "
                                + declaredPmtInfoCount
                                + ", Actual: "
                                + actualPmtInfoCount
                );
            }
            if (declaredPmtInfoControlSum != null
                    && declaredPmtInfoControlSum.compareTo(
                    actualPmtInfoControlSum) != 0) {

                throw new IllegalArgumentException(
                        "PmtInf control sum mismatch for "
                                + paymentInfoId
                                + ". Declared: "
                                + declaredPmtInfoControlSum
                                + ", Actual: "
                                + actualPmtInfoControlSum
                );
            }
        }

        if (declaredGroupCount != actualGroupCount) {
            throw new IllegalArgumentException(
                    "Transaction count mismatch. Declared: "
                            + declaredGroupCount
                            + ", Actual: "
                            + actualGroupCount
            );
        }

        if (declaredGroupControlSum != null
                && declaredGroupControlSum.compareTo(actualGroupControlSum) != 0) {

            throw new IllegalArgumentException(
                    "Group header control sum mismatch. Declared: "
                            + declaredGroupControlSum
                            + ", Actual: "
                            + actualGroupControlSum
            );
        }

        return new ExtractedPayment(messageId, transactions);

    }

    // Finds a direct child element using its namespace and local name
    private Element requiredChild(
            Element parent,
            String childName,
            String namespace
    ) {

        NodeList children = parent.getChildNodes();

        for (int i = 0; i < children.getLength(); i++) {

            Node node = children.item(i);

            if (node instanceof Element element
                    && childName.equals(element.getLocalName())
                    && namespace.equals(element.getNamespaceURI())) {

                return element;
            }
        }

        throw new IllegalArgumentException(
                "Missing required XML element: " + childName
        );
    }

    private String requiredText(
            Element parent,
            String childName,
            String namespace
    ) {

        String value = requiredChild(
                parent, childName, namespace
        ).getTextContent().trim();

        if (value.isBlank()) {
            throw new IllegalArgumentException(
                    "Empty XML element: " + childName
            );
        }

        return value;
    }

    private String optionalText(
            Element parent,
            String childName,
            String namespace
    ) {

        NodeList children = parent.getChildNodes();

        for (int i = 0; i < children.getLength(); i++) {

            Node node = children.item(i);

            if (node instanceof Element element
                    && childName.equals(element.getLocalName())
                    && namespace.equals(element.getNamespaceURI())) {

                String value = element.getTextContent().trim();

                if (value.isBlank()) {
                    throw new IllegalArgumentException(
                            "Empty XML element: " + childName
                    );
                }

                return value;
            }
        }

        return null;
    }

    // Internal extracted payment data
    public record ExtractedPayment(
            String messageId,
            List<ExtractedTransaction> transactions
    ) {
    }


    public record ExtractedTransaction(
            String paymentInfoId,
            String endToEndId,
            BigDecimal amount,
            String currency
    ) {
    }
}
