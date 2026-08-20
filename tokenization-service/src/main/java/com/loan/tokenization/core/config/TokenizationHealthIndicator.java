package com.loan.tokenization.core.config;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Custom actuator health indicator exposing service status
 * (architecture_pattern.md #34 — health checks/observability baseline).
 *
 * <p>Note: no hard-coded version here (see anti-pattern list in the
 * settlement-service analysis); details are limited to static service facts.</p>
 */
@Component
public class TokenizationHealthIndicator implements HealthIndicator {

    @Override
    public Health health() {
        return Health.up()
                .withDetail("service", "tokenization-service")
                .withDetail("engine", "FF1")
                .build();
    }
}
