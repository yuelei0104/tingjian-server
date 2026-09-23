package com.tingjian.server.dto;

import java.time.LocalDateTime;

public record KeywordResponse(
        String id,
        String phrase,
        boolean vibrationEnabled,
        int priority,
        boolean enabled,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
