package com.loan.tokenization.service;

import com.loan.tokenization.config.TokenizationProperties;
import com.loan.tokenization.dto.DataType;
import com.loan.tokenization.exception.DataValidationException;
import com.loan.tokenization.exception.UnsupportedDataTypeException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link FF1TokenizationEngine}.
 *
 * <p>The engine under test uses a fixed, test-only key and tweak so the tests
 * are deterministic. The production key lives in {@code application.yml}
 * (temporary development configuration, to be moved to Vault in a later
 * phase).</p>
 */
class FF1TokenizationEngineTest {

    private FF1TokenizationEngine engine;

    @BeforeEach
    void setUp() {
        engine = new FF1TokenizationEngine(testProperties());
    }

    private static TokenizationProperties testProperties() {
        TokenizationProperties properties = new TokenizationProperties();
        properties.getFf1().setKeyBase64("JAeCWt65h+Dk4rOgn3MLUXleTNhGH/nB0GN0eBFAHko=");
        properties.getFf1().setTweakHex("f83524b7eb52a221");
        return properties;
    }

    // --- Format ---

    @Test
    void token_hasSameLengthAsInput() {
        String token = engine.tokenize("9841234567", DataType.MOBILE);

        assertEquals(10, token.length());
    }

    @Test
    void token_containsDigitsOnly() {
        String token = engine.tokenize("9841234567", DataType.MOBILE);

        assertTrue(token.chars().allMatch(Character::isDigit));
    }

    // --- Determinism ---

    @Test
    void sameInput_alwaysProducesSameToken() {
        String first = engine.tokenize("9841234567", DataType.MOBILE);
        String second = engine.tokenize("9841234567", DataType.MOBILE);

        assertEquals(first, second);
    }

    // --- Different values ---

    @Test
    void differentInputs_produceDifferentTokens() {
        String first = engine.tokenize("9841234567", DataType.MOBILE);
        String second = engine.tokenize("9841234568", DataType.MOBILE);

        assertNotEquals(first, second);
    }

    // --- Reversibility ---

    @ParameterizedTest
    @ValueSource(strings = {"9841234567", "9841234568", "1234567890", "0000000001", "8765432109"})
    void detokenize_returnsOriginalValue(String value) {
        String token = engine.tokenize(value, DataType.MOBILE);

        assertEquals(value, engine.detokenize(token, DataType.MOBILE));
    }

    // --- Invalid values ---

    @Test
    void nullValue_isRejected() {
        assertThrows(DataValidationException.class,
                () -> engine.tokenize(null, DataType.MOBILE));
    }

    @Test
    void emptyValue_isRejected() {
        assertThrows(DataValidationException.class,
                () -> engine.tokenize("", DataType.MOBILE));
    }

    @Test
    void valueTooShort_isRejected() {
        assertThrows(DataValidationException.class,
                () -> engine.tokenize("98412345", DataType.MOBILE));
    }

    @Test
    void valueTooLong_isRejected() {
        assertThrows(DataValidationException.class,
                () -> engine.tokenize("98412345678", DataType.MOBILE));
    }

    @Test
    void valueWithLetters_isRejected() {
        assertThrows(DataValidationException.class,
                () -> engine.tokenize("98A1234567", DataType.MOBILE));
    }

    @Test
    void valueWithSpecialCharacters_isRejected() {
        assertThrows(DataValidationException.class,
                () -> engine.tokenize("98-1234567", DataType.MOBILE));
    }

    @Test
    void nullDataType_isRejected() {
        assertThrows(UnsupportedDataTypeException.class,
                () -> engine.tokenize("9841234567", null));
    }

    @Test
    void detokenize_rejectsInvalidToken() {
        assertThrows(DataValidationException.class,
                () -> engine.detokenize("98A1234567", DataType.MOBILE));
    }
}
