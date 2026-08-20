package com.loan.tokenization.core.model;

import java.time.Instant;

/**
 * Error details embedded in the unified {@link ApiResponse} envelope.
 *
 * <p>Matches architecture_pattern.md #27 (centralized error handling, no stack
 * traces) and #28 (consistent response structure).</p>
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
