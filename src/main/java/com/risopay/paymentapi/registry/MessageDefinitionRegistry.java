
package com.risopay.paymentapi.registry;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

@Component
public class MessageDefinitionRegistry {

    private static final String PAIN_001_NAMESPACE =
            "urn:iso:std:iso:20022:tech:xsd:pain.001.001.03";

    private final Map<String, MessageDefinition> definitions =
            Map.of(
                    PAIN_001_NAMESPACE,
                    new MessageDefinition(
                            "pain.001.001.03",
                            "xsd/pain.001.001.03.xsd"
                    )
            );

    public Optional<MessageDefinition> findByNamespace(String namespace) {
        return Optional.ofNullable(definitions.get(namespace));
    }

    public record MessageDefinition(
            String messageType,
            String xsdResourcePath
    ) {
    }
}
