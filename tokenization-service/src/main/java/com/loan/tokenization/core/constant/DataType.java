package com.loan.tokenization.core.constant;

import java.util.Arrays;

/**
 * Data types supported by the tokenization service.
 *
 * <p>Phase 1/2 supports only {@code MOBILE}; additional types will be added in
 * later phases. Placed in {@code core.constant} (architecture_pattern.md #13 —
 * no magic strings) because it is part of the {@code TokenizationEngine}
 * contract shared by every feature.</p>
 */
public enum DataType {

    MOBILE;

    /**
     * Returns whether the given type name matches one of the supported data
     * types. Matching is case-insensitive.
     *
     * @param type the type name to check, may be {@code null}
     * @return {@code true} if the type is supported
     */
    public static boolean isSupported(String type) {
        return type != null
                && Arrays.stream(values()).anyMatch(dt -> dt.name().equalsIgnoreCase(type));
    }

    /**
     * Human-readable list of supported types, e.g. {@code [MOBILE]}.
     *
     * @return string representation of the supported types
     */
    public static String supportedTypes() {
        return Arrays.toString(values());
    }
}
