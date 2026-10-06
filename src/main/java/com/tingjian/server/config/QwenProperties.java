package com.tingjian.server.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class QwenProperties {
    private final String apiKey;
    private final String baseUrl;
    private final String model;
    private final Duration connectTimeout;
    private final Duration readTimeout;

    public QwenProperties(
            @Value("${tingjian.ai.qwen.api-key:}") String apiKey,
            @Value("${tingjian.ai.qwen.base-url:https://dashscope.aliyuncs.com/compatible-mode/v1}") String baseUrl,
            @Value("${tingjian.ai.qwen.model:qwen-flash}") String model,
            @Value("${tingjian.ai.qwen.connect-timeout:2s}") Duration connectTimeout,
            @Value("${tingjian.ai.qwen.read-timeout:3s}") Duration readTimeout) {
        this.apiKey = apiKey.strip();
        this.baseUrl = stripTrailingSlash(baseUrl);
        this.model = model.strip();
        this.connectTimeout = connectTimeout;
        this.readTimeout = readTimeout;
    }

    public String apiKey() {
        return apiKey;
    }

    public String baseUrl() {
        return baseUrl;
    }

    public String model() {
        return model;
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
