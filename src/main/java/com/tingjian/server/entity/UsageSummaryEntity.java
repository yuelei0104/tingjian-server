package com.tingjian.server.entity;

public record UsageSummaryEntity(
        long conversationCount,
        long messageCount,
        long textCharacterCount,
        long totalDurationSeconds) {
}
