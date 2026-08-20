package com.loan.tokenization.core.exception;

/**
 * Thrown when the requested data type is not supported by the tokenization
 * service.
 *
 * <p>Mapped to HTTP 400 Bad Request by {@link GlobalExceptionHandler}
 * (architecture_pattern.md #27).</p>
 */
public class UnsupportedDataTypeException extends RuntimeException {

    public UnsupportedDataTypeException(String message) {
        super(message);
    }
}
