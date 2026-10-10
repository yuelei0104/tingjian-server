package com.tingjian.aispeech;

import java.time.Instant;
import java.util.List;

public record AgentResponse(
        String requestId,
        String answer,
        String provider,
        boolean fallback,
        List<String> toolsUsed,
        int contextMessages,
        Instant createdAt) {

    public AgentResponse {
        toolsUsed = toolsUsed == null ? List.of() : List.copyOf(toolsUsed);
    }
}
