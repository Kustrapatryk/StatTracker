package com.stattracker.common;

import java.time.Clock;
import java.util.ArrayList;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class CommonConfig {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    OpenAPI statTrackerOpenApi() {
        return new OpenAPI().info(new Info()
                .title("StatTracker API")
                .version("v1")
                .description("NBA player stats for line tracking. Data: NBA.com"));
    }

    /**
     * Response DTOs always serialize every property (absent values as {@code null}), so every property is
     * required; nullability comes from {@code @Nullable}. Without this, generated TypeScript types would make
     * every field optional.
     */
    @Bean
    OpenApiCustomizer requiredPropertiesCustomizer() {
        return openApi -> openApi.getComponents().getSchemas().forEach((name, schema) -> {
            if (schema.getProperties() != null && !name.equals("ProblemDetail")) {
                schema.setRequired(new ArrayList<>(schema.getProperties().keySet()));
            }
        });
    }
}
