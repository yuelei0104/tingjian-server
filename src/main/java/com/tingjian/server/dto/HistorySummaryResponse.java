package com.tingjian.server.dto;

import java.util.List;

public record HistorySummaryResponse(
        String sessionId,
        String summary,
        int messageCount,
        String generatedBy,
        List<String> highlights,
        List<String> actionItems,
        List<String> keywords,
        String tone,
        boolean cached) {
}
