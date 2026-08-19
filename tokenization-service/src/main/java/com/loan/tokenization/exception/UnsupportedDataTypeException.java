package com.loan.tokenization.exception;

/**
 * Thrown when the requested data type is not supported by the tokenization
 * service.
 *
 * <p>Mapped to HTTP 400 Bad Request by {@link GlobalExceptionHandler}.</p>
 */
public class UnsupportedDataTypeException extends RuntimeException {

    public UnsupportedDataTypeException(String message) {
        super(message);
    }
}
