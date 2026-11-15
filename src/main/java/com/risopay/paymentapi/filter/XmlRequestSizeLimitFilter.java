package com.risopay.paymentapi.filter;

import com.risopay.paymentapi.input.ReplayableXmlInput;
import com.risopay.paymentapi.input.TemporaryFileReplayableXmlInput;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.Semaphore;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class XmlRequestSizeLimitFilter extends OncePerRequestFilter {

    private static final Logger logger =
            LoggerFactory.getLogger(XmlRequestSizeLimitFilter.class);

    private static final String PAYMENT_ENDPOINT =
            "/api/v1/payment-instructions";

    private final long maxRequestSize;
    private final Path stagingDirectory;
    private final Semaphore stagingSlots;

    public XmlRequestSizeLimitFilter(
            @Value("${risopay.api.max-xml-request-size:52428800}")
            long maxRequestSize,
            @Value("${risopay.api.xml-staging-directory:}")
            String stagingDirectory,
            @Value("${risopay.api.max-concurrent-staged-xml-requests:16}")
            int maxConcurrentStagedRequests) {

        if (maxRequestSize <= 0) {
            throw new IllegalArgumentException(
                    "XML request size limit must be greater than zero."
            );
        }

        if (maxConcurrentStagedRequests <= 0) {
            throw new IllegalArgumentException(
                    "Maximum concurrent staged XML requests must be greater than zero."
            );
        }

        this.maxRequestSize = maxRequestSize;
        this.stagingDirectory = stagingDirectory.isBlank()
                ? null
                : Path.of(stagingDirectory).toAbsolutePath().normalize();
        this.stagingSlots = new Semaphore(maxConcurrentStagedRequests, true);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {

        String requestPath = request.getRequestURI()
                .substring(request.getContextPath().length());

        return !("POST".equalsIgnoreCase(request.getMethod())
                && PAYMENT_ENDPOINT.equals(requestPath));
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        long contentLength = request.getContentLengthLong();

        if (contentLength > maxRequestSize) {
            sendPayloadTooLargeResponse(response);
            return;
        }

        if (!stagingSlots.tryAcquire()) {
            sendStagingCapacityExceededResponse(response);
            return;
        }

        ReplayableXmlInput requestInput = null;

        try {
            requestInput = stageRequestBody(request);
            request.setAttribute(
                    ReplayableXmlInput.REQUEST_ATTRIBUTE,
                    requestInput
            );
            filterChain.doFilter(request, response);
        } catch (RequestTooLargeException exception) {
            logger.info("Chunked XML request rejected because it exceeded the size limit.");
            sendPayloadTooLargeResponse(response);
        } finally {
            request.removeAttribute(ReplayableXmlInput.REQUEST_ATTRIBUTE);

            if (requestInput != null) {
                try {
                    requestInput.close();
                } catch (IOException exception) {
                    logger.warn("Unable to delete staged XML request input.", exception);
                }
            }

            stagingSlots.release();
        }
    }

    private ReplayableXmlInput stageRequestBody(
            HttpServletRequest request
    ) throws IOException {

        Path temporaryFile = createTemporaryFile();
        boolean staged = false;

        try (
                InputStream inputStream = request.getInputStream();
                OutputStream outputStream = Files.newOutputStream(
                        temporaryFile,
                        StandardOpenOption.WRITE,
                        StandardOpenOption.TRUNCATE_EXISTING
                )
        ) {
            byte[] buffer = new byte[8192];
            long totalBytesRead = 0;
            int bytesRead;

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                totalBytesRead += bytesRead;

                if (totalBytesRead > maxRequestSize) {
                    throw new RequestTooLargeException();
                }

                outputStream.write(buffer, 0, bytesRead);
            }

            staged = true;
            return new TemporaryFileReplayableXmlInput(
                    temporaryFile,
                    totalBytesRead
            );
        } finally {
            if (!staged) {
                Files.deleteIfExists(temporaryFile);
            }
        }
    }

    private Path createTemporaryFile() throws IOException {
        if (stagingDirectory == null) {
            return Files.createTempFile("risopay-xml-", ".xml");
        }

        Files.createDirectories(stagingDirectory);
        return Files.createTempFile(stagingDirectory, "risopay-xml-", ".xml");
    }

    private void sendPayloadTooLargeResponse(
            HttpServletResponse response
    ) throws IOException {

        response.setStatus(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE);
        response.setContentType("application/json");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        response.getWriter().write("""
                {
                    "status": 413,
                    "error": "Payload Too Large",
                    "errorCode": "REQUEST_TOO_LARGE",
                    "message": "Maximum permitted XML request size is 50 MB",
                    "details": [
                        "The submitted XML Payload exceeds the permitted file size limit."
                    ]
                }
                """);
    }

    private void sendStagingCapacityExceededResponse(
            HttpServletResponse response
    ) throws IOException {

        response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
        response.setContentType("application/json");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        response.getWriter().write("""
                {
                    "status": 503,
                    "error": "Service Unavailable",
                    "errorCode": "XML_STAGING_CAPACITY_EXCEEDED",
                    "message": "The XML request staging capacity is temporarily unavailable",
                    "details": [
                        "Please retry the request shortly."
                    ]
                }
                """);
    }

    private static class RequestTooLargeException extends IOException {
    }
}
