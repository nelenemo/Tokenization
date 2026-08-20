package com.loan.tokenization.tokenize.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Request payload for {@code POST /api/tokenize}.
 *
 * <p>For {@code MOBILE} values, {@code value} must contain exactly 10 digits
 * and nothing else. Country-specific validation is intentionally not applied
 * yet.</p>
 *
 * <p>{@code @NotBlank} rejects {@code null}, empty and whitespace-only values,
 * so a dedicated {@code @NotNull} is not needed.</p>
 *
 * @param value the value to tokenize
 * @param type  the data type of the value, e.g. {@code MOBILE}
 */
public record TokenizeRequest(
        @NotBlank(message = "value must not be blank")
        @Pattern(regexp = "\\d{10}", message = "value must contain exactly 10 digits and digits only")
        String value,

        @NotBlank(message = "type must not be blank")
        String type
) {
}
