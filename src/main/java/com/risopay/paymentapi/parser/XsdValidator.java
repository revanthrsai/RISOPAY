
package com.risopay.paymentapi.parser;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;

import javax.xml.XMLConstants;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.ValidatorHandler;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class XsdValidator {

    private static final String DISALLOW_DOCTYPE_DECL =
            "http://apache.org/xml/features/disallow-doctype-decl";

    private static final String EXTERNAL_GENERAL_ENTITIES =
            "http://xml.org/sax/features/external-general-entities";

    private static final String EXTERNAL_PARAMETER_ENTITIES =
            "http://xml.org/sax/features/external-parameter-entities";

    private static final String LOAD_EXTERNAL_DTD =
            "http://apache.org/xml/features/nonvalidating/load-external-dtd";

    // Cache compiled schemas using their classpath resource paths.
    private final ConcurrentMap<String, Schema> schemaCache =
            new ConcurrentHashMap<>();

    public void validate(
            InputStream xmlInput,
            String xsdResourcePath
    ) throws IOException, SAXException {

        // 1. Retrieve the cached schema or compile it if not available.
        Schema schema = getOrLoadSchema(xsdResourcePath);

        // 2. Create a fresh event-based validator for every request.
        ValidatorHandler validatorHandler = schema.newValidatorHandler();

        validatorHandler.setErrorHandler(new ValidationErrorHandler());

        // 3. Feed the validator only secure SAX events from the XML input.
        XMLReader xmlReader = createSecureXmlReader();
        xmlReader.setContentHandler(validatorHandler);
        xmlReader.setErrorHandler(new ValidationErrorHandler());

        xmlReader.parse(new InputSource(xmlInput));
    }

    private XMLReader createSecureXmlReader() throws SAXException {

        SAXParserFactory parserFactory = SAXParserFactory.newInstance();
        parserFactory.setNamespaceAware(true);

        try {
            parserFactory.setFeature(
                    XMLConstants.FEATURE_SECURE_PROCESSING,
                    true
            );
            parserFactory.setFeature(DISALLOW_DOCTYPE_DECL, true);
            parserFactory.setFeature(EXTERNAL_GENERAL_ENTITIES, false);
            parserFactory.setFeature(EXTERNAL_PARAMETER_ENTITIES, false);
            parserFactory.setFeature(LOAD_EXTERNAL_DTD, false);

            XMLReader xmlReader = parserFactory.newSAXParser().getXMLReader();
            xmlReader.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            xmlReader.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");

            return xmlReader;
        } catch (ParserConfigurationException exception) {
            throw new SAXException(
                    "Unable to configure secure XML validation parser.",
                    exception
            );
        }
    }

    private static class ValidationErrorHandler implements ErrorHandler {

        @Override
        public void warning(SAXParseException exception) {
            // Keep warning handling consistent with the default parser behavior.
        }

        @Override
        public void error(SAXParseException exception) throws SAXException {
            throw exception;
        }

        @Override
        public void fatalError(SAXParseException exception) throws SAXException {
            throw exception;
        }
    }

    private Schema getOrLoadSchema(
            String xsdResourcePath
    ) throws IOException, SAXException {

        Schema cachedSchema = schemaCache.get(xsdResourcePath);

        if (cachedSchema != null) {
            return cachedSchema;
        }

        // Compile the schema only when it is not already cached.
        Schema compiledSchema = loadSchema(xsdResourcePath);

        // Store the compiled schema safely.
        Schema existingSchema = schemaCache.putIfAbsent(
                xsdResourcePath,
                compiledSchema
        );

        // If another thread cached it first, reuse that instance.
        return existingSchema != null
                ? existingSchema
                : compiledSchema;
    }

    private Schema loadSchema(
            String xsdResourcePath
    ) throws IOException, SAXException {

        // 1. Load the XSD from the application classpath.
        ClassPathResource xsdResource =
                new ClassPathResource(xsdResourcePath);

        // Temporary diagnostic log for cache testing.
        System.out.println(
                "Loading and compiling XSD: " + xsdResourcePath
        );

        // 2. Create an XSD SchemaFactory.
        SchemaFactory schemaFactory =
                SchemaFactory.newInstance(
                        XMLConstants.W3C_XML_SCHEMA_NS_URI
                );

        // 3. Apply XML security restrictions.
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

        // 4. Load and compile the XSD.
        try (InputStream xsdStream =
                     xsdResource.getInputStream()) {

            StreamSource xsdSource =
                    new StreamSource(xsdStream);

            return schemaFactory.newSchema(xsdSource);
        }
    }
}
