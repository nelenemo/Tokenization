package com.loan.tokenization.tokenize.service;

import com.loan.tokenization.core.constant.DataType;
import com.loan.tokenization.core.exception.DataValidationException;
import com.loan.tokenization.core.exception.UnsupportedDataTypeException;
import com.loan.tokenization.core.service.TokenizationEngine;
import com.loan.tokenization.tokenize.dto.TokenizeRequest;
import com.loan.tokenization.tokenize.dto.TokenizeResponse;
import com.loan.tokenization.tokenize.service.impl.TokenizationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link TokenizationServiceImpl}.
 */
class TokenizationServiceImplTest {

    private TokenizationService service;
    private TokenizationEngine engine;

    @BeforeEach
    void setUp() {
        engine = mock(TokenizationEngine.class);
        service = new TokenizationServiceImpl(engine);
    }

    @Test
    void validInput_delegatesToEngineAndReturnsToken() {
        when(engine.tokenize("9841234567", DataType.MOBILE)).thenReturn("4450187392");

        TokenizeResponse response = service.tokenize(new TokenizeRequest("9841234567", "MOBILE"));

        assertEquals("4450187392", response.token());
        verify(engine).tokenize("9841234567", DataType.MOBILE);
    }

    @Test
    void validInput_lowercaseType_isNormalizedToEnum() {
        service.tokenize(new TokenizeRequest("9841234567", "mobile"));

        verify(engine).tokenize("9841234567", DataType.MOBILE);
    }

    @Test
    void nullValue_isRejected() {
        assertThrows(DataValidationException.class,
                () -> service.tokenize(new TokenizeRequest(null, "MOBILE")));
        verifyNoInteractions(engine);
    }

    @Test
    void blankValue_isRejected() {
        assertThrows(DataValidationException.class,
                () -> service.tokenize(new TokenizeRequest("", "MOBILE")));
        verifyNoInteractions(engine);
    }

    @Test
    void valueTooShort_isRejected() {
        assertThrows(DataValidationException.class,
                () -> service.tokenize(new TokenizeRequest("98412345", "MOBILE")));
        verifyNoInteractions(engine);
    }

    @Test
    void valueTooLong_isRejected() {
        assertThrows(DataValidationException.class,
                () -> service.tokenize(new TokenizeRequest("98412345678", "MOBILE")));
        verifyNoInteractions(engine);
    }

    @Test
    void valueWithLetters_isRejected() {
        assertThrows(DataValidationException.class,
                () -> service.tokenize(new TokenizeRequest("98A1234567", "MOBILE")));
        verifyNoInteractions(engine);
    }

    @Test
    void valueWithSpecialCharacters_isRejected() {
        assertThrows(DataValidationException.class,
                () -> service.tokenize(new TokenizeRequest("98-1234567", "MOBILE")));
        verifyNoInteractions(engine);
    }

    @Test
    void nullType_isRejected() {
        assertThrows(DataValidationException.class,
                () -> service.tokenize(new TokenizeRequest("9841234567", null)));
        verifyNoInteractions(engine);
    }

    @Test
    void blankType_isRejected() {
        assertThrows(DataValidationException.class,
                () -> service.tokenize(new TokenizeRequest("9841234567", "")));
        verifyNoInteractions(engine);
    }

    @Test
    void unsupportedType_isRejected() {
        UnsupportedDataTypeException ex = assertThrows(UnsupportedDataTypeException.class,
                () -> service.tokenize(new TokenizeRequest("9841234567", "BANK")));

        assertEquals("Unsupported data type: BANK. Supported types: [MOBILE]", ex.getMessage());
        verifyNoInteractions(engine);
    }
}
