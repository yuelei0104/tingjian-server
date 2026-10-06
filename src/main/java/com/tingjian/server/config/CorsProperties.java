package com.tingjian.server.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
public class CorsProperties {
    private final String[] allowedOriginPatterns;

    public CorsProperties(@Value("${tingjian.cors.allowed-origin-patterns:http://localhost:*}") String value) {
        this.allowedOriginPatterns = Arrays.stream(value.split(","))
                .map(String::strip)
                .filter(origin -> !origin.isEmpty())
                .toArray(String[]::new);
    }

    public String[] allowedOriginPatterns() {
        return allowedOriginPatterns.clone();
    }
}
