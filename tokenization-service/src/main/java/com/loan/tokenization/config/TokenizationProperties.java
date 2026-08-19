package com.loan.tokenization.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration for the tokenization service.
 *
 * <p>Values are bound from the {@code app.tokenization} prefix in
 * {@code application.yml} using relaxed binding.</p>
 */
@ConfigurationProperties(prefix = "app.tokenization")
public class TokenizationProperties {

    private Ff1 ff1 = new Ff1();

    public Ff1 getFf1() {
        return ff1;
    }

    public void setFf1(Ff1 ff1) {
        this.ff1 = ff1;
    }

    /**
     * FF1 format-preserving encryption configuration.
     */
    public static class Ff1 {

        /**
         * TEMPORARY development-only AES key (Base64-encoded).
         *
         * <p>This key is supplied through configuration for local development
         * only and must be moved to HashiCorp Vault (Vault Transit) in a later
         * phase. It must decode to 16, 24 or 32 bytes (AES-128/192/256).</p>
         */
        private String keyBase64;

        /**
         * TEMPORARY development-only FF1 tweak (hex-encoded). Optional; an
         * empty tweak is allowed.
         */
        private String tweakHex = "";

        public String getKeyBase64() {
            return keyBase64;
        }

        public void setKeyBase64(String keyBase64) {
            this.keyBase64 = keyBase64;
        }

        public String getTweakHex() {
            return tweakHex;
        }

        public void setTweakHex(String tweakHex) {
            this.tweakHex = tweakHex;
        }
    }
}

