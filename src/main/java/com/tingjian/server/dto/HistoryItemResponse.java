package com.tingjian.server.dto;

import java.time.LocalDateTime;

public record HistoryItemResponse(
        String id,
        String title,
        String status,
        LocalDateTime startedAt,
        LocalDateTime endedAt,
        long messageCount,
        String preview) {
}
