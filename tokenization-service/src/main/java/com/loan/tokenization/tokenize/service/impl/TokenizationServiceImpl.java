package com.loan.tokenization.tokenize.service.impl;

import com.loan.tokenization.core.constant.DataType;
import com.loan.tokenization.core.exception.DataValidationException;
import com.loan.tokenization.core.exception.UnsupportedDataTypeException;
import com.loan.tokenization.core.service.TokenizationEngine;
import com.loan.tokenization.tokenize.dto.TokenizeRequest;
import com.loan.tokenization.tokenize.dto.TokenizeResponse;
import com.loan.tokenization.tokenize.service.TokenizationService;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

/**
 * Default {@link TokenizationService} implementation.
 *
 * <p>Owns the feature's business rules (validation) and delegates the actual
 * cryptographic transformation to the injected {@link TokenizationEngine}
 * (architecture_pattern.md #4 layering, #5 interface + impl).</p>
 */
@Service
public class TokenizationServiceImpl implements TokenizationService {



    private static final Pattern MOBILE_PATTERN = Pattern.compile("\\d{10}");

    private final TokenizationEngine engine;

    public TokenizationServiceImpl(TokenizationEngine engine) {
        this.engine = engine;
    }

    @Override
    public TokenizeResponse tokenize(TokenizeRequest request) {
        validate(request);
        String token = engine.tokenize(request.value(), DataType.valueOf(request.type().toUpperCase()));
        return new TokenizeResponse(token);
    }


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
