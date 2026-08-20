package com.loan.tokenization.core.service;

import com.loan.tokenization.core.constant.DataType;

/**
 * Abstraction over a format-preserving tokenization implementation.
 *
 * <p>Shared infrastructure contract (architecture_pattern.md #4/#5/#6):
 * the concrete cryptographic implementation can be swapped or extended
 * (e.g. FF1 today, dynamic/Vault-based later) without changing the feature
 * controllers or DTOs.</p>
 */
public interface TokenizationEngine {


    //Reversibly transforms a plaintext value into a token that keeps the
     // format of the original value (same length, same character set).

    String tokenize(String value, DataType dataType);

    /**
     * Reverses back to the original value.
     */
    String detokenize(String token, DataType dataType);
}
