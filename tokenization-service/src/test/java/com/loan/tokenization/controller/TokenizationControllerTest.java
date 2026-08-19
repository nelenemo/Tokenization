package com.loan.tokenization.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loan.tokenization.dto.TokenizeRequest;
import com.loan.tokenization.dto.TokenizeResponse;
import com.loan.tokenization.exception.UnsupportedDataTypeException;
import com.loan.tokenization.service.TokenizationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web slice tests for {@link TokenizationController}.
 */
@WebMvcTest(TokenizationController.class)
class TokenizationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TokenizationService tokenizationService;

    @Test
    void validRequest_returnsToken() throws Exception {
        when(tokenizationService.tokenize(any(TokenizeRequest.class)))
                .thenReturn(new TokenizeResponse("4450187392"));

        mockMvc.perform(post("/api/tokenize")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new TokenizeRequest("9841234567", "MOBILE"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("4450187392"));
    }

    @Test
    void missingValue_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/tokenize")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\": \"MOBILE\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("value must not be blank"));
    }

    @Test
    void nullValue_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/tokenize")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"value\": null, \"type\": \"MOBILE\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("value must not be blank"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "98412345",      // too short
            "98412345678",   // too long
            "98A1234567",    // letters
            "98-1234567",    // special characters
            ""               // empty
    })
    void invalidMobileFormat_returnsBadRequest(String invalidValue) throws Exception {
        mockMvc.perform(post("/api/tokenize")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"value\": \"" + invalidValue + "\", \"type\": \"MOBILE\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message", containsString("value")));
    }

    @Test
    void unsupportedType_returnsBadRequest() throws Exception {
        when(tokenizationService.tokenize(any(TokenizeRequest.class)))
                .thenThrow(new UnsupportedDataTypeException(
                        "Unsupported data type: BANK. Supported types: [MOBILE]"));

        mockMvc.perform(post("/api/tokenize")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"value\": \"9841234567\", \"type\": \"BANK\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message")
                        .value("Unsupported data type: BANK. Supported types: [MOBILE]"));
    }

    @Test
    void malformedJson_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/tokenize")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"value\": \"9841234567\", \"type\": \"MOBILE\""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Malformed JSON request body"));
    }
}
