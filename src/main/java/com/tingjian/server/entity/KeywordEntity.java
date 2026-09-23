package com.tingjian.server.entity;

import java.time.LocalDateTime;

public record KeywordEntity(
        String id,
        String ownerId,
        String phrase,
        boolean vibrationEnabled,
        int priority,
        boolean enabled,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
