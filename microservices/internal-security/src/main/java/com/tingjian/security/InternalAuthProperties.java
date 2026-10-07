package com.tingjian.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("tingjian.internal-auth")
public record InternalAuthProperties(boolean enabled, String token) {
    public InternalAuthProperties {
        token = token == null ? "" : token.strip();
    }
}
