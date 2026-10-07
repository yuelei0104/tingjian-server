package com.tingjian.security;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@EnableConfigurationProperties(InternalAuthProperties.class)
@ConditionalOnProperty(prefix = "tingjian.internal-auth", name = "enabled", havingValue = "true")
public class InternalSecurityAutoConfiguration {
    @Bean
    InternalServiceAuthFilter internalServiceAuthFilter(InternalAuthProperties properties) {
        if (properties.token().isBlank()) {
            throw new IllegalStateException(
                    "tingjian.internal-auth.token must be configured when internal auth is enabled");
        }
        return new InternalServiceAuthFilter(properties.token());
    }
}
