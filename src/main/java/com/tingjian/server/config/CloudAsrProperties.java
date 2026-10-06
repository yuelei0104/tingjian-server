package com.tingjian.server.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class CloudAsrProperties {
    private final String provider;
    private final String apiKey;
    private final String websocketUrl;
    private final String model;
    private final int maxSentenceSilence;

    public CloudAsrProperties(
            @Value("${tingjian.speech.asr.provider:android}") String provider,
            @Value("${tingjian.speech.aliyun.api-key:}") String apiKey,
            @Value("${tingjian.speech.aliyun.websocket-url:wss://dashscope.aliyuncs.com/api-ws/v1/inference}") String websocketUrl,
            @Value("${tingjian.speech.aliyun.asr-model:paraformer-realtime-v2}") String model,
            @Value("${tingjian.speech.aliyun.max-sentence-silence:800}") int maxSentenceSilence) {
        this.provider = provider.strip();
        this.apiKey = apiKey.strip();
        this.websocketUrl = websocketUrl.strip();
        this.model = model.strip();
        this.maxSentenceSilence = Math.max(200, Math.min(6000, maxSentenceSilence));
    }

    public boolean enabled() {
        return "aliyun".equalsIgnoreCase(provider)
                && !apiKey.isBlank()
                && websocketUrl.startsWith("wss://");
    }

    public String apiKey() {
        return apiKey;
    }

    public String websocketUrl() {
        return websocketUrl;
    }

    public String model() {
        return model;
    }

    public int maxSentenceSilence() {
        return maxSentenceSilence;
    }
}
