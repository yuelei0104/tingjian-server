package com.tingjian.server.dto;

public record UsageOverviewResponse(
        long conversationCount,
        long messageCount,
        long textCharacterCount,
        long totalDurationSeconds) {
}
