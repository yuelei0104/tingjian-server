package com.tingjian.aispeech;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProviderStatusService {
    private final String qwenApiKey;
    private final String qwenModel;
    private final String asrApiKey;
    private final String asrModel;

    public ProviderStatusService(
            @Value("${tingjian.ai.qwen.api-key:}") String qwenApiKey,
            @Value("${tingjian.ai.qwen.model:qwen-plus}") String qwenModel,
            @Value("${tingjian.speech.asr.api-key:}") String asrApiKey,
            @Value("${tingjian.speech.asr.model:paraformer-realtime-v2}") String asrModel) {
        this.qwenApiKey = qwenApiKey;
        this.qwenModel = qwenModel;
        this.asrApiKey = asrApiKey;
        this.asrModel = asrModel;
    }

    public ProviderStatusResponse current() {
        return new ProviderStatusResponse(List.of(
                new ProviderStatusResponse.ProviderCapability(
                        "AI", "QWEN", qwenModel, isConfigured(qwenApiKey), "CLOUD"),
                new ProviderStatusResponse.ProviderCapability(
                        "ASR", "DASHSCOPE", asrModel, isConfigured(asrApiKey), "CLOUD"),
                new ProviderStatusResponse.ProviderCapability(
                        "TTS", "ANDROID", "device-default", true, "ON_DEVICE")
        ));
    }

    private boolean isConfigured(String secret) {
        return secret != null && !secret.isBlank();
    }
}
