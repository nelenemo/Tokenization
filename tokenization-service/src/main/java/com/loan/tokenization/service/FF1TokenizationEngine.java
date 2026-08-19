package com.loan.tokenization.service;

import com.loan.tokenization.config.TokenizationProperties;
import com.loan.tokenization.dto.DataType;
import com.loan.tokenization.exception.DataValidationException;
import com.loan.tokenization.exception.UnsupportedDataTypeException;
import org.bouncycastle.crypto.fpe.FPEFF1Engine;
import org.bouncycastle.crypto.params.FPEParameters;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.util.encoders.Hex;
import org.springframework.stereotype.Service;

import java.util.Base64;
import java.util.regex.Pattern;

/**
 * FF1 format-preserving tokenization engine built on BouncyCastle
 * (NIST SP 800-38G, "FF1 Format-Preserving Encryption").
 *
 * <p>FF1 is a keyed permutation over a chosen alphabet. Because it is a
 * bijection on the domain, distinct plaintext values always map to distinct
 * tokens (satisfying the "different token per customer" requirement) while the
 * same value deterministically maps to the same token (required so that
 * detokenization works). The token keeps the exact format of the input:
 * for {@link DataType#MOBILE} that is 10 digits.</p>
 *
 * <p>The engine stores no data; detokenization is computed directly from the
 * FF1 inverse, so no database is needed. Database-backed token storage is a
 * later-phase concern.</p>
 *
 * <p>Thread-safety: a single {@link FPEFF1Engine} (and its underlying AES
 * cipher) is reused and guarded by synchronization.</p>
 */
@Service
public class FF1TokenizationEngine implements TokenizationEngine {

    /**
     * MOBILE values must contain exactly 10 digits and nothing else.
     * Country-specific rules are intentionally not applied yet.
     */
    private static final Pattern MOBILE_PATTERN = Pattern.compile("\\d{10}");

    /** FF1 radix for the decimal alphabet (10 symbols: 0-9). */
    private static final int RADIX = 10;

    /** Valid AES key sizes in bytes (AES-128/192/256). */
    private static final int[] VALID_KEY_SIZES = {16, 24, 32};

    private final FPEFF1Engine engine = new FPEFF1Engine();
    private final FPEParameters parameters;

    /**
     * Builds the FF1 engine from the (temporary, development-only) FF1
     * configuration. The AES key and tweak are intentionally not hard-coded in
     * source; they are supplied through configuration and will be moved to
     * HashiCorp Vault in a later phase.
     *
     * @param properties the tokenization configuration
     */
    public FF1TokenizationEngine(TokenizationProperties properties) {
        byte[] key = decodeKey(properties.getFf1().getKeyBase64());
        byte[] tweak = decodeTweak(properties.getFf1().getTweakHex());
        this.parameters = new FPEParameters(new KeyParameter(key), RADIX, tweak);
    }

    @Override
    public String tokenize(String value, DataType dataType) {
        validate(value, dataType);
        return transform(value, true);
    }

    @Override
    public String detokenize(String token, DataType dataType) {
        validate(token, dataType);
        return transform(token, false);
    }

    /**
     * Runs the value through FF1. Input bytes must be the digit values (0-9);
     * the BouncyCastle engine uses the byte-optimised path for radix {@code <= 256}.
     */
    private String transform(String value, boolean encrypt) {
        byte[] in = toDigitValues(value);
        byte[] out = new byte[in.length];
        synchronized (engine) {
            engine.init(encrypt, parameters);
            engine.processBlock(in, 0, in.length, out, 0);
        }
        return toDigitString(out);
    }

    /**
     * Defensive validation so the engine is safe to call even when the
     * upper-layer (DTO {@code @Valid} / service) checks are bypassed.
     */
    private void validate(String value, DataType dataType) {
        if (dataType == null) {
            throw new UnsupportedDataTypeException(
                    "data type must not be null. Supported types: " + DataType.supportedTypes());
        }
        if (!DataType.isSupported(dataType.name())) {
            throw new UnsupportedDataTypeException(
                    "Unsupported data type: " + dataType
                            + ". Supported types: " + DataType.supportedTypes());
        }
        if (value == null || value.isBlank()) {
            throw new DataValidationException("value must not be null or empty");
        }
        if (!MOBILE_PATTERN.matcher(value).matches()) {
            throw new DataValidationException("value must contain exactly 10 digits and digits only");
        }
    }

    /**
     * Converts each character of a numeric string to its base-10 digit value
     * (0-9) stored in one byte, as expected by the FF1 engine's byte path.
     */
    private static byte[] toDigitValues(String value) {
        byte[] out = new byte[value.length()];
        for (int i = 0; i < value.length(); i++) {
            out[i] = (byte) (value.charAt(i) - '0');
        }
        return out;
    }

    /**
     * Converts FF1 output bytes (digit values 0-9) back into a digit string.
     */
    private static String toDigitString(byte[] values) {
        StringBuilder sb = new StringBuilder(values.length);
        for (byte b : values) {
            sb.append((char) ('0' + (b & 0xFF)));
        }
        return sb.toString();
    }

    private static byte[] decodeKey(String keyBase64) {
        byte[] key = Base64.getDecoder().decode(keyBase64);
        boolean valid = false;
        for (int size : VALID_KEY_SIZES) {
            if (key.length == size) {
                valid = true;
                break;
            }
        }
        if (!valid) {
            throw new IllegalArgumentException(
                    "FF1 key must be 16, 24 or 32 bytes (AES-128/192/256)");
        }
        return key;
    }

    private static byte[] decodeTweak(String tweakHex) {
        if (tweakHex == null || tweakHex.isBlank()) {
            return new byte[0]; // FF1 tweak is optional and may be empty
        }
        return Hex.decode(tweakHex);
    }
}
