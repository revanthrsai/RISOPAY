package com.risopay.paymentapi.filter;

import com.risopay.paymentapi.input.ReplayableXmlInput;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.InputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class XmlRequestSizeLimitFilterTest {

    private static final String ENDPOINT =
            "/api/v1/payment-instructions";

    @TempDir
    Path stagingDirectory;

    @Test
    void stagesRequestOnDiskAndDeletesItAfterSuccessfulProcessing()
            throws Exception {

        byte[] xml = "<Document>payment</Document>".getBytes();
        XmlRequestSizeLimitFilter filter = newFilter(100);
        MockHttpServletRequest request = requestWithBody(xml);
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean chainInvoked = new AtomicBoolean(false);

        filter.doFilter(request, response, (filteredRequest, filteredResponse) -> {
            chainInvoked.set(true);

            ReplayableXmlInput input = (ReplayableXmlInput) filteredRequest
                    .getAttribute(ReplayableXmlInput.REQUEST_ATTRIBUTE);

            assertEquals(xml.length, input.sizeInBytes());
            assertArrayEquals(xml, readAll(input));
            assertArrayEquals(xml, readAll(input));
            assertEquals(1, stagedFileCount());
        });

        assertTrue(chainInvoked.get());
        assertEquals(200, response.getStatus());
        assertEquals(0, stagedFileCount());
        assertFalse(Files.exists(stagingDirectory.resolve("unknown.xml")));
    }

    @Test
    void deletesStagedFileWhenDownstreamProcessingThrows() throws Exception {

        XmlRequestSizeLimitFilter filter = newFilter(100);
        MockHttpServletRequest request = requestWithBody("<Document/>".getBytes());
        MockHttpServletResponse response = new MockHttpServletResponse();

        try {
            filter.doFilter(request, response, (ignoredRequest, ignoredResponse) -> {
                assertEquals(1, stagedFileCount());
                throw new IllegalStateException("simulated parser failure");
            });
        } catch (IllegalStateException exception) {
            assertEquals("simulated parser failure", exception.getMessage());
        }

        assertEquals(0, stagedFileCount());
    }

    @Test
    void rejectsOversizedDeclaredContentLengthWithoutInvokingChain()
            throws Exception {

        XmlRequestSizeLimitFilter filter = newFilter(10);
        MockHttpServletRequest request = new MockHttpServletRequest() {
            @Override
            public long getContentLengthLong() {
                return 11;
            }
        };
        request.setMethod("POST");
        request.setRequestURI(ENDPOINT);
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean chainInvoked = new AtomicBoolean(false);

        filter.doFilter(request, response, (ignoredRequest, ignoredResponse) ->
                chainInvoked.set(true));

        assertFalse(chainInvoked.get());
        assertEquals(413, response.getStatus());
        assertTrue(response.getContentAsString().contains("REQUEST_TOO_LARGE"));
        assertEquals(0, stagedFileCount());
    }

    @Test
    void rejectsChunkedRequestAfterActualBytesExceedLimit() throws Exception {

        XmlRequestSizeLimitFilter filter = newFilter(10);
        MockHttpServletRequest request = new MockHttpServletRequest() {
            @Override
            public int getContentLength() {
                return -1;
            }

            @Override
            public long getContentLengthLong() {
                return -1;
            }
        };
        request.setMethod("POST");
        request.setRequestURI(ENDPOINT);
        request.setContent("12345678901".getBytes());
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean chainInvoked = new AtomicBoolean(false);

        filter.doFilter(request, response, (ignoredRequest, ignoredResponse) ->
                chainInvoked.set(true));

        assertFalse(chainInvoked.get());
        assertEquals(413, response.getStatus());
        assertTrue(response.getContentAsString().contains("REQUEST_TOO_LARGE"));
        assertEquals(0, stagedFileCount());
    }

    private XmlRequestSizeLimitFilter newFilter(long maxSize) {
        return new XmlRequestSizeLimitFilter(
                maxSize,
                stagingDirectory.toString(),
                1
        );
    }

    private MockHttpServletRequest requestWithBody(byte[] body) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("POST");
        request.setRequestURI(ENDPOINT);
        request.setContent(body);
        return request;
    }

    private byte[] readAll(ReplayableXmlInput input) throws IOException {
        try (InputStream stream = input.openStream()) {
            return stream.readAllBytes();
        }
    }

    private long stagedFileCount() throws IOException {
        try (var files = Files.list(stagingDirectory)) {
            return files.count();
        }
    }
}
