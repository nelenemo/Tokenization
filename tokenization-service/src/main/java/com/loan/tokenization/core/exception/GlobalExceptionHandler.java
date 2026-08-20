package com.loan.tokenization.core.exception;

import com.loan.tokenization.core.model.ApiError;
import com.loan.tokenization.core.model.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

/**
 * Central exception handler that converts exceptions into a consistent
 * {@link ApiResponse} envelope (architecture_pattern.md #27 + #28).
 *
 * <p>Internal stack traces are never exposed through the API.</p>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Bean Validation failures (invalid, missing or malformed request fields).
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(
            MethodArgumentNotValidException ex, HttpServletRequest request) {

        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(FieldError::getDefaultMessage)
                .orElse("Validation failed");

        return build(HttpStatus.BAD_REQUEST, message, request);
    }

    /**
     * Malformed JSON / unreadable request body, including a missing body.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnreadableMessage(
            HttpMessageNotReadableException ex, HttpServletRequest request) {

        return build(HttpStatus.BAD_REQUEST, "Malformed JSON request body", request);
    }

    /**
     * Business validation failures thrown by the service layer.
     */
    @ExceptionHandler(DataValidationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataValidation(
            DataValidationException ex, HttpServletRequest request) {

        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    /**
     * Requests for data types the tokenization service does not support.
     */
    @ExceptionHandler(UnsupportedDataTypeException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnsupportedDataType(
            UnsupportedDataTypeException ex, HttpServletRequest request) {

        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    /**
     * Fallback for anything unexpected.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneric(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception while processing request {} {}", request.getMethod(), request.getRequestURI(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred", request);
    }

    private ResponseEntity<ApiResponse<Void>> build(HttpStatus status, String message, HttpServletRequest request) {
        ApiError apiError = new ApiError(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                request.getRequestURI()
        );
        return ResponseEntity.status(status).body(ApiResponse.failure(apiError));
    }
}
