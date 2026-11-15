
package com.risopay.paymentapi.exception;

import com.risopay.paymentapi.dto.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import org.xml.sax.SAXParseException;
import org.springframework.web.HttpMediaTypeNotSupportedException;

import java.time.Instant;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger =
            LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // 1. Handle malformed XML
    @ExceptionHandler(SAXParseException.class)
    public ResponseEntity<ApiErrorResponse> handleMalformedXml(
            SAXParseException exception,
            HttpServletRequest request) {

        return buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                "INVALID_XML_STRUCTURE",
                "The submitted XML is malformed.",
                "XML File is incomplete or incorrectly formatted.",
                request
        );
    }

    // 2. Handle existing controller exceptions
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiErrorResponse> handleResponseStatusException(
            ResponseStatusException exception,
            HttpServletRequest request) {

        String reason = exception.getReason() == null
                ? ""
                : exception.getReason();

        int originalStatus = exception.getStatusCode().value();

        // Unsupported ISO 20022 message
        if (reason.startsWith("Unsupported ISO 20022")) {

            return buildErrorResponse(
                    HttpStatus.BAD_REQUEST,
                    "UNSUPPORTED_MESSAGE_TYPE",
                    "The submitted ISO 20022 message type is not supported.",
                    "The message namespace is not registered in ISO 20022.",
                    request
            );
        }

        // XSD validation failure
        if (reason.startsWith("XML validation failed")) {

            return buildErrorResponse(
                    HttpStatus.BAD_REQUEST,
                    "XSD_VALIDATION_FAILED",
                    "The submitted XML failed XSD validation.",
                    "The XML document does not align with ISO 20022 schema.",
                    request
            );
        }

        // Duplicate submission
        if (originalStatus == 409) {

            return buildErrorResponse(
                    HttpStatus.CONFLICT,
                    "DUPLICATE_FILE_PROCESSED",
                    "This payment instruction has already been processed.",
                    "A duplicate payment instruction was detected.",
                    request
            );
        }

        // Business validation failure
        if (originalStatus == 422 || originalStatus == 400) {

            return buildErrorResponse(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "BUSINESS_VALIDATION_FAILED",
                    "Payment instruction failed business validation.",
                    reason.isBlank()
                            ? "One or more payment validation rules failed."
                            : reason,
                    request
            );
        }

        // Preserve other explicitly assigned HTTP statuses
        HttpStatus status = HttpStatus.resolve(originalStatus);

        if (status == null) {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        }

        return buildErrorResponse(
                status,
                status.is5xxServerError()
                        ? "INTERNAL_SERVER_ERROR"
                        : "REQUEST_REJECTED",
                status.is5xxServerError()
                        ? "An unexpected error occurred."
                        : "The request could not be processed.",
                "Please check the request and try again.",
                request
        );
    }

    // 3. Handle unsupported request Content-Type
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiErrorResponse> handleUnsupportedMediaType(
            HttpMediaTypeNotSupportedException exception,
            HttpServletRequest request) {

        return buildErrorResponse(
                HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                "UNSUPPORTED_MEDIA_TYPE",
                "The submitted request Content-Type is not supported.",
                "Please submit payment instructions using application/xml.",
                request
        );
    }

    // 4. Handle unexpected application exceptions
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGeneralException(
            Exception exception,
            HttpServletRequest request) {

        logger.error(
                "Unexpected application error at {}",
                request.getRequestURI(),
                exception
        );

        return buildErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "INTERNAL_SERVER_ERROR",
                "An unexpected error occurred.",
                "Please contact the RISOPAY support team.",
                request
        );
    }

    // 5. Build the standard error response
    private ResponseEntity<ApiErrorResponse> buildErrorResponse(
            HttpStatus status,
            String errorCode,
            String message,
            String detail,
            HttpServletRequest request) {

        Instant timestamp = Instant.now();

        ApiErrorResponse response = new ApiErrorResponse(
                timestamp,
                status.value(),
                status.getReasonPhrase(),
                errorCode,
                message,
                request.getRequestURI(),
                List.of(detail)
        );

        // Generate a structured failure audit log event
        logger.warn(
                "AUDIT_EVENT eventCode={} eventType=FAILURE timestamp={} path={} description={}",
                errorCode,
                timestamp,
                request.getRequestURI(),
                message
        );

        return ResponseEntity
                .status(status)
                .body(response);
    }
}
