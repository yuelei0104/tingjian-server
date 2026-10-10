package com.tingjian.server.dto;

import java.time.Instant;
import java.util.List;

public record AgentChatResponse(
        String clientRequestId,
        String answer,
        String provider,
        boolean fallback,
        List<String> toolsUsed,
        int contextMessages,
        Instant createdAt) {

    public AgentChatResponse {
        toolsUsed = toolsUsed == null ? List.of() : List.copyOf(toolsUsed);
    }
}
