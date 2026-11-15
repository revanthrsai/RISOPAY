package com.risopay.paymentapi.input;

import java.io.IOException;
import java.io.InputStream;

/**
 * A request-scoped XML source that can be read repeatedly without retaining
 * the original request body in application heap.
 */
public interface ReplayableXmlInput extends AutoCloseable {

    String REQUEST_ATTRIBUTE = ReplayableXmlInput.class.getName();

    InputStream openStream() throws IOException;

    long sizeInBytes();

    @Override
    void close() throws IOException;
}
