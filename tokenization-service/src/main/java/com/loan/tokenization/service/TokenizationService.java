package com.loan.tokenization.service;

import com.loan.tokenization.dto.DataType;
import com.loan.tokenization.dto.TokenizeRequest;
import com.loan.tokenization.dto.TokenizeResponse;
import com.loan.tokenization.exception.DataValidationException;
import com.loan.tokenization.exception.UnsupportedDataTypeException;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

/**
 * Entry point for tokenization requests.
 *
 * <p>Validates the request and delegates the actual format-preserving
 * transformation to a {@link TokenizationEngine} (currently FF1).</p>
 */
@Service
public class TokenizationService {

    /**
     * MOBILE values must contain exactly 10 digits and nothing else.
     * Country-specific rules are intentionally not applied yet.
     */
    private static final Pattern MOBILE_PATTERN = Pattern.compile("\\d{10}");

    private final TokenizationEngine engine;

    public TokenizationService(TokenizationEngine engine) {
        this.engine = engine;
    }

    /**
     * Validates and tokenizes the request.
     *
     * @param request the tokenization request
     * @return the tokenization response
     * @throws DataValidationException      if the value or type is null/blank or the value format is invalid
     * @throws UnsupportedDataTypeException if the data type is not supported
     */
    public TokenizeResponse tokenize(TokenizeRequest request) {
        validate(request);
        String token = engine.tokenize(request.value(), DataType.valueOf(request.type().toUpperCase()));
        return new TokenizeResponse(token);
    }

    /**
     * Validates the request in the service layer so the service is safe to call
     * even when the DTO-level Bean Validation ({@code @Valid}) is bypassed.
     */
    private void validate(TokenizeRequest request) {
        if (request == null || request.value() == null || request.value().isBlank()) {
            throw new DataValidationException("value must not be null or empty");
        }
        if (request.type() == null || request.type().isBlank()) {
            throw new DataValidationException("type must not be null or empty");
        }
        if (!DataType.isSupported(request.type())) {
            throw new UnsupportedDataTypeException(
                    "Unsupported data type: " + request.type()
                            + ". Supported types: " + DataType.supportedTypes());
        }
        // MOBILE is the only supported type at this stage, so the format check
        // below applies to every accepted request.
        if (!MOBILE_PATTERN.matcher(request.value()).matches()) {
            throw new DataValidationException("value must contain exactly 10 digits and digits only");
        }
    }
}

