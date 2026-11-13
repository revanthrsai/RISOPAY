
package com.risopay.paymentapi.parser;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;

@Component
public class XsdValidator {

    public void validate(
            String xmlPayload,
            String xsdResourcePath
    ) throws IOException, SAXException {

        // 1. Load the XSD from the application classpath
        ClassPathResource xsdResource =
                new ClassPathResource(xsdResourcePath);

        // 2. Create an XSD schema factory
        SchemaFactory schemaFactory =
                SchemaFactory.newInstance(
                        XMLConstants.W3C_XML_SCHEMA_NS_URI
                );

        // 3. Apply XML security restrictions
        schemaFactory.setFeature(
                XMLConstants.FEATURE_SECURE_PROCESSING,
                true
        );

        schemaFactory.setProperty(
                XMLConstants.ACCESS_EXTERNAL_DTD,
                ""
        );

        schemaFactory.setProperty(
                XMLConstants.ACCESS_EXTERNAL_SCHEMA,
                ""
        );

        // 4. Load and compile the XSD
        Schema schema;

        try (InputStream xsdStream = xsdResource.getInputStream()) {

            StreamSource xsdSource = new StreamSource(xsdStream);

            schema = schemaFactory.newSchema(xsdSource);
        }

        // 5. Create a validator using the loaded XSD
        Validator validator = schema.newValidator();

        validator.setProperty(
                XMLConstants.ACCESS_EXTERNAL_DTD,
                ""
        );

        validator.setProperty(
                XMLConstants.ACCESS_EXTERNAL_SCHEMA,
                ""
        );

        // 6. Validate the incoming XML
        StreamSource xmlSource =
                new StreamSource(new StringReader(xmlPayload));

        validator.validate(xmlSource);
    }
}
