package com.loan.tokenization.dto;

import java.time.Instant;

/**
 * Consistent error response body used by the global exception handler.
 *
 * @param timestamp when the error occurred
 * @param status    HTTP status code
 * @param error     HTTP status reason phrase
 * @param message   human-readable error description
 * @param path      request path that produced the error
 */
public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path
) {
}
