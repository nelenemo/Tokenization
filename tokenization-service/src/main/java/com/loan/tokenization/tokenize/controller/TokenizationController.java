package com.loan.tokenization.tokenize.controller;

import com.loan.tokenization.core.model.ApiResponse;
import com.loan.tokenization.tokenize.dto.TokenizeRequest;
import com.loan.tokenization.tokenize.dto.TokenizeResponse;
import com.loan.tokenization.tokenize.service.TokenizationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api")
public class TokenizationController {

    private final TokenizationService tokenizationService;

    public TokenizationController(TokenizationService tokenizationService) {
        this.tokenizationService = tokenizationService;
    }

 
    @PostMapping("/tokenize")
    public ResponseEntity<ApiResponse<TokenizeResponse>> tokenize(@Valid @RequestBody TokenizeRequest request) {
        return ResponseEntity.ok(ApiResponse.success(tokenizationService.tokenize(request)));
    }
}
