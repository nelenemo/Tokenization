package com.loan.tokenization.core.model;

import org.springframework.http.HttpStatus;

/**
 * Unified response envelope used by every API endpoint
 * (architecture_pattern.md #28: {@code status, message, data, error}).
 *
 * <p>Controllers never return different shapes for similar operations:
 * success responses carry {@code data} and {@code error == null}, error
 * responses carry {@code data == null} and a populated {@code error}.</p>
 *
 * @param status  HTTP status code
 * @param message human-readable result/error message
 * @param data    payload on success, {@code null} on error
 * @param error   error details on error, {@code null} on success
 * @param <T>     type of the success payload
 */
public record ApiResponse<T>(
        int status,
        String message,
        T data,
        ApiError error
) {

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(HttpStatus.OK.value(), "Success", data, null);
    }

    public static ApiResponse<Void> failure(ApiError error) {
        return new ApiResponse<>(error.status(), error.message(), null, error);
    }
}
