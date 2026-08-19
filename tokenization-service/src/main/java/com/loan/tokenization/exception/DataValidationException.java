package com.loan.tokenization.exception;

/**
 * Thrown when a request field fails business validation in the service layer.
 *
 * <p>Mapped to HTTP 400 Bad Request by {@link GlobalExceptionHandler}.</p>
 */
public class DataValidationException extends RuntimeException {

    public DataValidationException(String message) {
        super(message);
    }
}
