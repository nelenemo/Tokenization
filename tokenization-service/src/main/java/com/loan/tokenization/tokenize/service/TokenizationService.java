package com.loan.tokenization.tokenize.service;

import com.loan.tokenization.tokenize.dto.TokenizeRequest;
import com.loan.tokenization.tokenize.dto.TokenizeResponse;

public interface TokenizationService {


    TokenizeResponse tokenize(TokenizeRequest request);
}
