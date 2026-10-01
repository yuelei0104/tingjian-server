package com.tingjian.server.dto;

public record RealtimeMessageRequest(
        String type,
        String sessionId,
        String clientMessageId,
        SessionMessageRequest.Speaker speaker,
        String content) {
}
