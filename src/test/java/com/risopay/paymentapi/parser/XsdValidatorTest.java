package com.risopay.paymentapi.parser;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class XsdValidatorTest {

    private static final String XSD_RESOURCE =
            "xsd/pain.001.001.03.xsd";

    private static final String PAIN_001_NAMESPACE =
            "urn:iso:std:iso:20022:tech:xsd:pain.001.001.03";

    private final XsdValidator validator = new XsdValidator();

    @Test
    void validatesSupportedPaymentXml() throws Exception {

        ClassPathResource xmlResource = new ClassPathResource(
                "fixtures/pain001/valid-single-transaction.xml"
        );

        try (InputStream xmlInput = xmlResource.getInputStream()) {
            assertDoesNotThrow(() ->
                    validator.validate(xmlInput, XSD_RESOURCE));
        }
    }

    @Test
    void rejectsXmlThatViolatesTheRegisteredXsd() {

        String xml = """
                <Document xmlns="%s">
                    <CstmrCdtTrfInitn/>
                </Document>
                """.formatted(PAIN_001_NAMESPACE);

        assertValidationFails(xml);
    }

    @Test
    void rejectsDoctypeDeclarations() {

        String xml = """
                <!DOCTYPE Document [<!ENTITY test "blocked">]>
                <Document xmlns="%s">&test;</Document>
                """.formatted(PAIN_001_NAMESPACE);

        assertValidationFails(xml);
    }

    @Test
    void rejectsExternalEntityDeclarations() {

        String xml = """
                <!DOCTYPE Document [
                    <!ENTITY external SYSTEM "file:///private-data.txt">
                ]>
                <Document xmlns="%s">&external;</Document>
                """.formatted(PAIN_001_NAMESPACE);

        assertValidationFails(xml);
    }

    private void assertValidationFails(String xml) {
        InputStream xmlInput = new ByteArrayInputStream(
                xml.getBytes(StandardCharsets.UTF_8)
        );

        assertThrows(
                SAXException.class,
                () -> validator.validate(xmlInput, XSD_RESOURCE)
        );
    }
}
