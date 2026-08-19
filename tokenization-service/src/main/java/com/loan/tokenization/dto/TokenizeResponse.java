package com.loan.tokenization.dto;

/**
 * Response payload for {@code POST /api/tokenize}.
 *
 * @param token the format-preserving token for the requested value
 */
public record TokenizeResponse(String token) {
}

