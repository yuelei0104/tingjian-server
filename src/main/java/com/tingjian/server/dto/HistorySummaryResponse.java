package com.tingjian.server.dto;

public record HistorySummaryResponse(
        String sessionId,
        String summary,
        int messageCount,
        String generatedBy) {
}
