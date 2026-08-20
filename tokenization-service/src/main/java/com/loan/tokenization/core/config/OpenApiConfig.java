package com.loan.tokenization.core.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * springdoc OpenAPI documentation for the service
 * (architecture_pattern.md #35 — API documentation reflects the implementation).
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI tokenizationOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Tokenization Service")
                        .version("0.0.1-SNAPSHOT")
                        .description("Loan tokenization microservice. POST /api/tokenize returns "
                                + "a format-preserving token for a validated value."));
    }
}
