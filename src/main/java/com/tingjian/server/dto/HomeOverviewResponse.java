package com.tingjian.server.dto;

public record HomeOverviewResponse(
        long conversationCount,
        long messageCount,
        long totalDurationSeconds) {
}
