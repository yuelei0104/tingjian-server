package com.tingjian.server.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class AiSpeechServiceProperties {
    private final boolean enabled;
    private final String baseUrl;
    private final String internalToken;
    private final Duration connectTimeout;
    private final Duration readTimeout;

    public AiSpeechServiceProperties(
            @Value("${tingjian.microservices.ai-speech.enabled:false}") boolean enabled,
            @Value("${tingjian.microservices.ai-speech.base-url:http://127.0.0.1:8092}") String baseUrl,
            @Value("${tingjian.microservices.ai-speech.internal-token:}") String internalToken,
            @Value("${tingjian.microservices.ai-speech.connect-timeout:1s}") Duration connectTimeout,
            @Value("${tingjian.microservices.ai-speech.read-timeout:30s}") Duration readTimeout) {
        this.enabled = enabled;
        this.baseUrl = stripTrailingSlash(baseUrl);
        this.internalToken = internalToken.strip();
        this.connectTimeout = connectTimeout;
        this.readTimeout = readTimeout;
    }

    public boolean enabled() {
        return enabled;
    }

    public String baseUrl() {
        return baseUrl;
    }

    public String internalToken() {
        return internalToken;
    }

    public Duration connectTimeout() {
        return connectTimeout;
    }

    public Duration readTimeout() {
        return readTimeout;
    }

    private static String stripTrailingSlash(String value) {
        String result = value.strip();
        while (result.endsWith("/")) result = result.substring(0, result.length() - 1);
        return result;
    }
}
