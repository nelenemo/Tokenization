package com.loan.tokenization.service;

import com.loan.tokenization.dto.DataType;

/**
 * Abstraction over a format-preserving tokenization implementation.
 *
 * <p>Delegating real token generation here lets the cryptographic implementation
 * (currently FF1) be replaced later without changing the controllers or the
 * request/response DTOs.</p>
 */
public interface TokenizationEngine {

    /**
     * Reversibly transforms a plaintext value into a token that keeps the
     * format of the original value (same length, same character set).
     *
     * @param value    the plaintext value to tokenize
     * @param dataType the data type of the value
     * @return the format-preserving token
     */
    String tokenize(String value, DataType dataType);

    /**
     * Reverses {@link #tokenize(String, DataType)} back to the original value.
     *
     * @param token    the token to detokenize
     * @param dataType the data type of the token
     * @return the original value
     */
    String detokenize(String token, DataType dataType);
}
