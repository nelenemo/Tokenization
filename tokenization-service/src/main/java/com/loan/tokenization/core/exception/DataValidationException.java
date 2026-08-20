package com.loan.tokenization.core.exception;

/**
 * Thrown when a request field fails business validation in the service layer.
 *
 * <p>Mapped to HTTP 400 Bad Request by {@link GlobalExceptionHandler}
 * (architecture_pattern.md #27 — typed exceptions + centralized handling).</p>
 */
public class DataValidationException extends RuntimeException {

    public DataValidationException(String message) {
        super(message);
    }
}
