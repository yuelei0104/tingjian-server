package com.tingjian.server.entity;

import java.time.LocalDateTime;

public record HistorySummaryEntity(
        String id,
        String title,
        String status,
        LocalDateTime startedAt,
        LocalDateTime endedAt,
        long messageCount,
        String preview) {
}
