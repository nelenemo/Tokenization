package com.loan.tokenization.controller;

import com.loan.tokenization.dto.TokenizeRequest;
import com.loan.tokenization.dto.TokenizeResponse;
import com.loan.tokenization.service.TokenizationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST endpoints for tokenization.
 */
@RestController
@RequestMapping("/api")
public class TokenizationController {

    private final TokenizationService tokenizationService;

    public TokenizationController(TokenizationService tokenizationService) {
        this.tokenizationService = tokenizationService;
    }

    /**
     * Tokenizes a value using the configured format-preserving engine.
     *
     * @param request the value and its data type
     * @return the tokenization response containing the generated token
     */
    @PostMapping("/tokenize")
    public ResponseEntity<TokenizeResponse> tokenize(@Valid @RequestBody TokenizeRequest request) {
        return ResponseEntity.ok(tokenizationService.tokenize(request));
    }
}
